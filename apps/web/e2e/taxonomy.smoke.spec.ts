import {
  expect,
  test,
  type APIRequestContext,
  type Page,
} from '@playwright/test';

import { en } from './messages';

import {
  findVerificationToken,
  registerVerifyLogin,
  uniqueSmokeUser,
  type SmokeUser,
} from './helpers';
import { provisionReviewer } from './staff-bootstrap';

/**
 * Phase-8 optical-taxonomy admin smoke. Drives the real stack (this app,
 * the Spring Boot API on :8080, Mailpit on :8025). The platform admin is
 * provisioned via the documented staff-bootstrap fixture with the
 * PLATFORM_ADMIN role, which grants TAXONOMY_WRITE + BRAND_APPROVE. No taxonomy
 * state is faked in the DB — every category/attribute/brand is created through
 * the admin UI under test.
 */

const API_BASE = process.env.PLAYWRIGHT_API_URL ?? 'http://localhost:8080/api/v1';
const ta = en.taxonomyAdmin;

/** Register via the API to capture the user id (needed for staff provisioning). */
async function apiRegister(
  request: APIRequestContext,
  user: SmokeUser,
): Promise<string> {
  const response = await request.post(`${API_BASE}/auth/register`, {
    data: {
      email: user.email,
      password: user.password,
      displayName: user.displayName,
    },
  });
  expect(response.ok()).toBeTruthy();
  const body = (await response.json()) as { userId: string };
  return body.userId;
}

async function verifyEmail(
  page: Page,
  request: APIRequestContext,
  user: SmokeUser,
): Promise<void> {
  const token = await findVerificationToken(request, user.email);
  await page.goto(`/en/auth/verify-email?token=${encodeURIComponent(token)}`);
  await expect(page.getByText(en.auth.verifyEmail.successTitle)).toBeVisible({
    timeout: 15_000,
  });
}

async function login(page: Page, user: SmokeUser): Promise<void> {
  await page.goto('/en/auth/login');
  await page.waitForLoadState('networkidle');
  await page.getByLabel(en.auth.login.emailLabel).fill(user.email);
  await page.getByLabel(en.auth.login.passwordLabel).fill(user.password);
  await page.getByRole('button', { name: en.auth.login.submit }).click();
  await expect(page).toHaveURL(/\/en\/buyer/, { timeout: 15_000 });
}

/** Reload /{locale}/taxonomy until `assertion` passes (async admin commits). */
async function reloadUntil(
  page: Page,
  locale: string,
  assertion: (page: Page) => Promise<void>,
): Promise<void> {
  await expect(async () => {
    await page.goto(`/${locale}/taxonomy`);
    await assertion(page);
  }).toPass({ timeout: 30_000 });
}

// ---------------------------------------------------------------------------
// Journey A/B/C — platform admin manages the taxonomy registry.
// ---------------------------------------------------------------------------
test.describe('taxonomy administration', () => {
  test('A/B/C: create category tree, attribute, template, brand, certification; localize; nest @smoke', async ({
    page,
    request,
  }) => {
    test.setTimeout(300_000);

    const stamp = Date.now();
    const parentCode = `E2E_FRAME_${stamp}`;
    const childCode = `E2E_FRAME_METAL_${stamp}`;
    const attrCode = `E2E_ATTR_${stamp}`;
    const brandCode = `E2E_BRAND_${stamp}`;
    const certCode = `E2E_CERT_${stamp}`;
    const templateCode = `E2E_TMPL_${stamp}`;
    const parentName = `Frames ${stamp}`;
    const childName = `Metal frames ${stamp}`;
    const translatedName = `Monturas ${stamp}`;

    const admin = uniqueSmokeUser('taxonomy.admin', 'Taxonomy Admin');
    const adminId = await apiRegister(request, admin);
    await verifyEmail(page, request, admin);
    await provisionReviewer(adminId, { role: 'PLATFORM_ADMIN' });
    await login(page, admin);

    // Open the admin dashboard.
    await page.goto('/en/taxonomy');
    await expect(
      page.getByRole('heading', { name: ta.dashboard.title }),
    ).toBeVisible();

    // A1: create a top-level category.
    const catForm = page.locator(`form[aria-label="${ta.categories.create.title}"]`);
    await catForm.getByLabel(ta.categories.create.codeLabel).fill(parentCode);
    await catForm.getByLabel(ta.categories.create.nameLabel).fill(parentName);
    await catForm.getByRole('button', { name: ta.categories.create.submit }).click();
    await reloadUntil(page, 'en', async (p) => {
      await expect(
        p.locator(`[data-category-code="${parentCode}"]`),
      ).toBeVisible({ timeout: 3_000 });
    });

    // A2 + hierarchy: create a child under the parent and confirm it nests.
    const catForm2 = page.locator(`form[aria-label="${ta.categories.create.title}"]`);
    await catForm2.getByLabel(ta.categories.create.codeLabel).fill(childCode);
    await catForm2.getByLabel(ta.categories.create.nameLabel).fill(childName);
    await catForm2.getByLabel(ta.categories.create.parentLabel).selectOption(parentCode);
    await catForm2.getByRole('button', { name: ta.categories.create.submit }).click();
    await reloadUntil(page, 'en', async (p) => {
      await expect(
        p.locator(`[data-category-code="${parentCode}"] [data-category-code="${childCode}"]`),
      ).toBeVisible({ timeout: 3_000 });
    });

    // A3: create an attribute.
    const attrForm = page.locator(`form[aria-label="${ta.attributes.create.title}"]`);
    await attrForm.getByLabel(ta.attributes.create.codeLabel).fill(attrCode);
    await attrForm.getByLabel(ta.attributes.create.nameLabel).fill(`Attribute ${stamp}`);
    await attrForm.getByRole('button', { name: ta.attributes.create.submit }).click();
    await reloadUntil(page, 'en', async (p) => {
      await expect(
        p.locator(`[data-attribute-code="${attrCode}"]`),
      ).toBeVisible({ timeout: 3_000 });
    });

    // A4: create a specification template attaching the attribute to the parent.
    const tmplForm = page.locator(`form[aria-label="${ta.specificationTemplate.title}"]`);
    const categoryValue = await tmplForm
      .locator('#tmpl-category option', { hasText: parentCode })
      .getAttribute('value');
    expect(categoryValue).toBeTruthy();
    await tmplForm.locator('#tmpl-category').selectOption(categoryValue!);
    await tmplForm
      .getByLabel(ta.specificationTemplate.templateCodeLabel)
      .fill(templateCode);
    await tmplForm.locator('#tmpl-attr-0').selectOption(attrCode);
    await tmplForm
      .getByRole('button', { name: ta.specificationTemplate.submit })
      .click();
    await expect(page.getByText(ta.specificationTemplate.saved)).toBeVisible({
      timeout: 15_000,
    });

    // A5: create a brand and approve it.
    const brandForm = page.locator(`form[aria-label="${ta.brands.create.title}"]`);
    await brandForm.getByLabel(ta.brands.create.codeLabel).fill(brandCode);
    await brandForm
      .getByLabel(ta.brands.create.canonicalNameLabel)
      .fill(`Brand ${stamp}`);
    await brandForm.getByRole('button', { name: ta.brands.create.submit }).click();
    await reloadUntil(page, 'en', async (p) => {
      await expect(p.locator(`[data-brand-code="${brandCode}"]`)).toBeVisible({
        timeout: 3_000,
      });
    });
    const brandRow = page.locator(`[data-brand-code="${brandCode}"]`);
    await brandRow.getByRole('button', { name: ta.brands.approval.approve }).click();
    await expect(brandRow.locator('[data-brand-status="APPROVED"]')).toBeVisible({
      timeout: 15_000,
    });

    // A6: create a certification.
    const certForm = page.locator(`form[aria-label="${ta.certifications.create.title}"]`);
    await certForm.getByLabel(ta.certifications.create.codeLabel).fill(certCode);
    await certForm
      .getByLabel(ta.certifications.create.nameLabel)
      .fill(`Certification ${stamp}`);
    await certForm.getByRole('button', { name: ta.certifications.create.submit }).click();
    await reloadUntil(page, 'en', async (p) => {
      await expect(
        p.locator(`[data-certification-code="${certCode}"]`),
      ).toBeVisible({ timeout: 3_000 });
    });

    // B: add a Spanish translation for the parent category, then approve it.
    const parentNode = page.locator(`[data-category-code="${parentCode}"]`);
    await parentNode
      .getByRole('button', { name: ta.translations.title })
      .first()
      .click();
    const trForm = page.locator(`form[aria-label="${ta.translations.title}"]`);
    await trForm.getByLabel(ta.translations.localeLabel).selectOption('es');
    await trForm.getByLabel(ta.translations.nameLabel).fill(translatedName);
    await trForm.getByRole('button', { name: ta.translations.submit }).click();
    await expect(page.getByText(ta.translations.saved)).toBeVisible({ timeout: 15_000 });
    await trForm.getByRole('button', { name: ta.translations.approve }).click();
    await expect(page.getByText(ta.translations.approved)).toBeVisible({
      timeout: 15_000,
    });

    // B (verify): the translated name renders under the es locale. Scope to the parent's
    // tree node — the localized name also legitimately appears in the parent-category and
    // template <option> dropdowns, so an unscoped getByText matches multiple elements.
    await reloadUntil(page, 'es', async (p) => {
      await expect(
        p.locator(`[data-category-code="${parentCode}"]`).getByText(translatedName, { exact: true }),
      ).toBeVisible({ timeout: 3_000 });
    });
  });
});

// ---------------------------------------------------------------------------
// Gating — a non-staff user is forbidden from the taxonomy admin surface.
// ---------------------------------------------------------------------------
test.describe('taxonomy access control', () => {
  test('a registered non-staff user hitting /en/taxonomy gets the forbidden state @smoke', async ({
    browser,
    request,
  }) => {
    test.setTimeout(180_000);

    const outsider = uniqueSmokeUser('taxonomy.outsider', 'Taxonomy Outsider');
    const outsiderCtx = await browser.newContext();
    const outsiderPage = await outsiderCtx.newPage();

    try {
      await registerVerifyLogin(outsiderPage, request, outsider);
      await outsiderPage.goto('/en/taxonomy');
      await expect(outsiderPage.getByText(ta.forbidden.title)).toBeVisible();
    } finally {
      await outsiderCtx.close();
    }
  });
});
