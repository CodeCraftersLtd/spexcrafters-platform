import { expect, test, type Page } from '@playwright/test';

import { en } from './messages';

import { registerVerifyLogin, uniqueSmokeUser } from './helpers';

/**
 * Phase-6 CSRF smoke (ADR-018). Self-contained via registerVerifyLogin.
 * Verifies the end-to-end token flow through the real UI (which now sends
 * X-CSRF-Token via csrfFetch), then probes the BFF directly to prove the guard
 * rejects forged mutations. Requires the backend API (:8080) and Mailpit
 * (:8025). Uses only public boundaries — no DB access.
 */

const orgCopy = en.organizations;

async function createOrganizationViaUi(page: Page, name: string): Promise<string> {
  await page.goto('/en/organizations/new');
  await page.waitForLoadState('networkidle');
  await page.getByLabel(orgCopy.create.nameLabel).fill(name);
  await page.getByLabel(orgCopy.create.typeLabel).selectOption('BUYER');
  await page.getByRole('button', { name: orgCopy.create.submit }).click();

  const workspaceUrl =
    /\/en\/organizations\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})$/;
  await page.waitForURL(workspaceUrl, { timeout: 15_000 });
  const match = page.url().match(workspaceUrl);
  if (!match?.[1]) {
    throw new Error(`could not extract organization id from ${page.url()}`);
  }
  return match[1];
}

test.describe('CSRF protection', () => {
  test('token flow: UI mutation works, forged mutations are blocked @smoke', async ({
    page,
    request,
    baseURL,
  }) => {
    test.setTimeout(180_000);
    const origin = (baseURL ?? 'http://localhost:3000').replace(/\/$/, '');

    const user = uniqueSmokeUser('csrf.owner', 'CSRF Smoke Owner');
    await registerVerifyLogin(page, request, user);

    // The JS-readable sc_csrf cookie is present; the credential stays HttpOnly.
    const cookies = await page.context().cookies();
    const csrf = cookies.find((c) => c.name === 'sc_csrf');
    const sessionCookie = cookies.find((c) => c.name === 'sc_session');
    expect(csrf?.value, 'sc_csrf cookie should be set at login').toBeTruthy();
    expect(csrf?.httpOnly, 'sc_csrf must be JS-readable').toBe(false);
    expect(sessionCookie?.httpOnly, 'sc_session must stay HttpOnly').toBe(true);
    const csrfToken = csrf!.value;

    // Happy path: creating an org through the real UI (csrfFetch attaches the
    // header) succeeds.
    const orgName = `CSRF Smoke Org ${Date.now()}`;
    const orgId = await createOrganizationViaUi(page, orgName);
    expect(orgId).toMatch(/^[0-9a-f-]{36}$/);

    // page.request shares the browser context cookies (sc_session + sc_csrf).
    // 1) No X-CSRF-Token header → 403 csrf (origin is valid, so the token layer
    // is what rejects).
    const noToken = await page.request.post('/api/orgs', {
      headers: { origin, 'content-type': 'application/json' },
      data: { name: 'Forged NoToken', type: 'BUYER' },
    });
    expect(noToken.status()).toBe(403);
    expect((await noToken.json()).error.code).toBe('csrf');

    // 2) Wrong token → 403.
    const wrongToken = await page.request.post('/api/orgs', {
      headers: {
        origin,
        'content-type': 'application/json',
        'x-csrf-token': 'not-the-real-token',
      },
      data: { name: 'Forged WrongToken', type: 'BUYER' },
    });
    expect(wrongToken.status()).toBe(403);
    expect((await wrongToken.json()).error.code).toBe('csrf');

    // 3) Mismatched Origin, even with the real token → 403 (origin layer).
    const badOrigin = await page.request.post('/api/orgs', {
      headers: {
        origin: 'http://evil.example',
        'content-type': 'application/json',
        'x-csrf-token': csrfToken,
      },
      data: { name: 'Forged BadOrigin', type: 'BUYER' },
    });
    expect(badOrigin.status()).toBe(403);
    expect((await badOrigin.json()).error.code).toBe('csrf');

    // 4) Safe GET /api/auth/session is unaffected by the guard → 200.
    const session = await page.request.get('/api/auth/session');
    expect(session.status()).toBe(200);

    // Logout through the UI (csrfFetch attaches the token) succeeds and clears
    // both cookies.
    await page.goto(`/en/organizations/${orgId}`);
    await page.getByRole('button', { name: orgCopy.logout }).click();
    await expect(page).toHaveURL(/\/en$/, { timeout: 15_000 });

    const afterLogout = await page.context().cookies();
    expect(afterLogout.find((c) => c.name === 'sc_csrf')).toBeUndefined();
    expect(afterLogout.find((c) => c.name === 'sc_session')).toBeUndefined();

    // Post-logout mutation: no session → 401 (auth fails before the CSRF layer).
    const postLogout = await page.request.post('/api/orgs', {
      headers: { origin, 'content-type': 'application/json' },
      data: { name: 'After Logout', type: 'BUYER' },
    });
    expect(postLogout.status()).toBe(401);
  });
});
