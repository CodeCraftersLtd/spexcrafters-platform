import { expect, test } from '@playwright/test';

import { en } from './messages';

import { findVerificationToken, uniqueSmokeUser } from './helpers';

/**
 * Walking-skeleton smoke: registration → email verification (via Mailpit) →
 * login → buyer dashboard → logout. Requires the backend API (:8080) and
 * Mailpit (:8025) to be running alongside the web app.
 */

test.describe('auth vertical slice', () => {
  test('register → verify → login → dashboard → logout @smoke', async ({
    page,
    request,
  }) => {
    const user = uniqueSmokeUser('smoke', 'Smoke Test Buyer');

    // 1. Register. Wait for hydration before interacting: a pre-hydration
    // click would trigger a native (non-RHF) submit instead of the client
    // handler. Real users hit this window too — the forms carry
    // method="post" so a native submit can never leak fields into the URL.
    await page.goto('/en/auth/register');
    await page.waitForLoadState('networkidle');
    await expect(
      page.getByRole('heading', { name: en.auth.register.title }),
    ).toBeVisible();

    await page.getByLabel(en.auth.register.displayNameLabel).fill(user.displayName);
    await page.getByLabel(en.auth.register.emailLabel).fill(user.email);
    await page.getByLabel(en.auth.register.passwordLabel).fill(user.password);
    await page
      .getByRole('button', { name: en.auth.register.submit })
      .click();

    await expect(page.getByText(en.auth.register.checkEmail.title)).toBeVisible();

    // 2. Pull the verification link from Mailpit.
    const token = await findVerificationToken(request, user.email);

    // 3. Follow the verification link.
    await page.goto(`/en/auth/verify-email?token=${encodeURIComponent(token)}`);
    await expect(
      page.getByText(en.auth.verifyEmail.successTitle),
    ).toBeVisible({ timeout: 15_000 });

    await page
      .getByRole('link', { name: en.auth.verifyEmail.goToLogin })
      .click();
    await expect(page).toHaveURL(/\/en\/auth\/login/);
    await page.waitForLoadState('networkidle');

    // 4. Log in.
    await page.getByLabel(en.auth.login.emailLabel).fill(user.email);
    await page.getByLabel(en.auth.login.passwordLabel).fill(user.password);
    await page.getByRole('button', { name: en.auth.login.submit }).click();

    // 5. Buyer dashboard greets the user by display name.
    await expect(page).toHaveURL(/\/en\/buyer/, { timeout: 15_000 });
    await expect(
      page.getByRole('heading', { name: `Welcome, ${user.displayName}` }),
    ).toBeVisible();

    // 6. Log out and confirm the session guard re-engages.
    await page.getByRole('button', { name: en.buyer.logout }).click();
    await expect(page).toHaveURL(/\/en$/, { timeout: 15_000 });

    await page.goto('/en/buyer');
    await expect(page).toHaveURL(/\/en\/auth\/login/);
  });

  test('buyer portal redirects signed-out visitors to login @smoke', async ({
    page,
  }) => {
    await page.goto('/en/buyer');
    await expect(page).toHaveURL(/\/en\/auth\/login\?returnTo=/);
    await expect(
      page.getByRole('heading', { name: en.auth.login.title }),
    ).toBeVisible();
  });

  test('root path negotiates a locale prefix @smoke', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/\/(en|zh-CN|fr|de|ar)(\/|$)/);
    await expect(
      page.getByRole('heading', { name: en.home.hero.title }),
    ).toBeVisible();
  });
});
