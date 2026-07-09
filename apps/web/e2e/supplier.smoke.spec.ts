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
 * Phase-7 supplier + reviewer smoke (Journeys A–G, brief §62). Drives the real
 * stack: this app (next dev), the Spring Boot API (:8080), Mailpit (:8025), and
 * MinIO for evidence storage. Self-contained — every identity is registered by
 * the test. The reviewer is provisioned via the documented staff-bootstrap
 * fixture (platform_access.platform_staff); no application/supplier state is
 * ever faked in the DB.
 */

const API_BASE = process.env.PLAYWRIGHT_API_URL ?? 'http://localhost:8080/api/v1';
const UUID = /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/;

const s = en.suppliers;
const r = en.reviewer;

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

async function login(page: Page, user: SmokeUser, locale = 'en'): Promise<void> {
  await page.goto(`/${locale}/auth/login`);
  await page.waitForLoadState('networkidle');
  await page.getByLabel(en.auth.login.emailLabel).fill(user.email);
  await page.getByLabel(en.auth.login.passwordLabel).fill(user.password);
  await page.getByRole('button', { name: en.auth.login.submit }).click();
  await expect(page).toHaveURL(new RegExp(`/${locale}/buyer`), { timeout: 15_000 });
}

async function createOrganization(
  page: Page,
  name: string,
  locale = 'en',
): Promise<string> {
  await page.goto(`/${locale}/organizations/new`);
  await page.waitForLoadState('networkidle');
  await page.getByLabel(en.organizations.create.nameLabel).fill(name);
  await page.getByLabel(en.organizations.create.typeLabel).selectOption('SUPPLIER');
  await page.getByRole('button', { name: en.organizations.create.submit }).click();
  const pattern = new RegExp(`/${locale}/organizations/(${UUID.source})$`);
  await page.waitForURL(pattern, { timeout: 15_000 });
  return page.url().match(pattern)![1]!;
}

/** Start a supplier application; returns the application id from the URL. */
async function startApplication(
  page: Page,
  legalName: string,
  locale = 'en',
): Promise<string> {
  await page.goto(`/${locale}/supplier`);
  await page.waitForLoadState('networkidle');
  await page.getByLabel(s.home.legalNameLabel).fill(legalName);
  await page.getByRole('button', { name: s.home.start }).click();
  const pattern = new RegExp(`/${locale}/supplier/applications/(${UUID.source})$`);
  await page.waitForURL(pattern, { timeout: 15_000 });
  return page.url().match(pattern)![1]!;
}

/** Attach a small PDF as evidence via the initiate→PUT→finalize flow. */
async function uploadEvidence(page: Page): Promise<void> {
  // Exact match: a facility visibility checkbox's label also loosely matches
  // the file label; the evidence input's accessible name is exactly it.
  await page.getByLabel(en.evidence.fileLabel, { exact: true }).setInputFiles({
    name: 'registration.pdf',
    mimeType: 'application/pdf',
    buffer: Buffer.from('%PDF-1.4\n%smoke-test-evidence\n'),
  });
  await expect(page.getByText(en.evidence.uploaded)).toBeVisible({ timeout: 30_000 });
}

// ---------------------------------------------------------------------------
// Journey A — global supplier onboarding.
// ---------------------------------------------------------------------------
test.describe('supplier onboarding', () => {
  test('A: onboarding → company info → optical profile → content → evidence → submit @smoke', async ({
    page,
    request,
  }) => {
    test.setTimeout(240_000);
    const user = uniqueSmokeUser('supplier.a', 'Supplier A Owner');
    await registerVerifyLogin(page, request, user);
    await createOrganization(page, `Acme Optics ${Date.now()}`);

    await startApplication(page, 'Acme Optics Manufacturing Ltd');
    await expect(page.getByRole('heading', { name: s.application.title })).toBeVisible();

    // Company info + optical profile (types + capabilities).
    await page.getByLabel(s.companyInfo.registrationNumberLabel).fill('CN-123456');
    await page.getByLabel(s.companyInfo.countryLabel).fill('CN');
    // Exact: "Lens manufacturer" is a substring of "Contact lens manufacturer".
    await page
      .getByLabel(en.taxonomy.supplierType.LENS_MANUFACTURER, { exact: true })
      .check();
    await page.getByLabel(en.taxonomy.capability.LENS_COATING, { exact: true }).check();
    await page.getByRole('button', { name: s.companyInfo.save }).click();
    await expect(page.getByText(s.companyInfo.saved)).toBeVisible({ timeout: 15_000 });

    // Original-language content.
    await page
      .getByLabel(s.content.companyDescriptionLabel)
      .fill('Full-service optical lens manufacturer.');
    await page.getByRole('button', { name: s.content.save }).first().click();

    // Facility.
    await page.getByLabel(s.facilities.countryLabel, { exact: true }).fill('CN');
    await page.getByRole('button', { name: s.facilities.add }).click();
    await expect(page.getByText(s.facilities.added)).toBeVisible({ timeout: 15_000 });

    // Evidence upload (direct-to-storage).
    await uploadEvidence(page);

    // Submit for review.
    await page.getByRole('button', { name: s.review.submit }).click();
    await expect(page.getByText(s.review.submitted)).toBeVisible({ timeout: 15_000 });
    await expect(page.getByText(s.status.state.SUBMITTED, { exact: true }).first()).toBeVisible();
  });
});

// ---------------------------------------------------------------------------
// Journeys B/C/D — reviewer, remediation, approval + scope grant.
// ---------------------------------------------------------------------------
test.describe('reviewer workflow', () => {
  test('B/C/D: claim → request changes → supplier resubmits → approve → grant scope @smoke', async ({
    browser,
    request,
  }) => {
    test.setTimeout(300_000);

    const supplier = uniqueSmokeUser('supplier.d', 'Supplier D Owner');
    const reviewer = uniqueSmokeUser('reviewer.d', 'Reviewer D');

    const supplierCtx = await browser.newContext();
    const reviewerCtx = await browser.newContext();
    const supplierPage = await supplierCtx.newPage();
    const reviewerPage = await reviewerCtx.newPage();

    try {
      // Supplier submits an application (Journey A, condensed).
      await registerVerifyLogin(supplierPage, request, supplier);
      await createOrganization(supplierPage, `Bril Optics ${Date.now()}`);
      const applicationId = await startApplication(supplierPage, 'Bril Optics Ltd');
      // Completeness requires registration number, country, ≥1 type and ≥1 capability.
      await supplierPage.getByLabel(s.companyInfo.registrationNumberLabel).fill('VN-987654');
      await supplierPage.getByLabel(s.companyInfo.countryLabel).fill('VN');
      await supplierPage
        .getByLabel(en.taxonomy.supplierType.LENS_MANUFACTURER, { exact: true })
        .check();
      await supplierPage
        .getByLabel(en.taxonomy.capability.LENS_COATING, { exact: true })
        .check();
      await supplierPage.getByRole('button', { name: s.companyInfo.save }).click();
      await expect(supplierPage.getByText(s.companyInfo.saved)).toBeVisible({ timeout: 15_000 });
      await uploadEvidence(supplierPage);
      await supplierPage.getByRole('button', { name: s.review.submit }).click();
      await expect(supplierPage.getByText(s.review.submitted)).toBeVisible({ timeout: 15_000 });

      // Reviewer is provisioned via the documented staff-bootstrap fixture.
      const reviewerId = await apiRegister(request, reviewer);
      await verifyEmail(reviewerPage, request, reviewer);
      await provisionReviewer(reviewerId, { role: 'SENIOR_REVIEWER' });
      await login(reviewerPage, reviewer);

      // B: reviewer opens the queue and the detail.
      await reviewerPage.goto('/en/reviewer');
      await expect(reviewerPage.getByRole('heading', { name: r.queue.title })).toBeVisible();
      await reviewerPage.goto(`/en/reviewer/${applicationId}`);
      await reviewerPage.getByRole('button', { name: r.actions.claim }).click();

      // C: reviewer requests changes; supplier responds + resubmits.
      await reviewerPage.getByRole('button', { name: r.actions.requestChanges }).click();
      await reviewerPage.getByLabel(r.requestChanges.requestedItemLabel).fill('Registration');
      await reviewerPage.getByLabel(r.requestChanges.reasonLabel).fill('Please add the number.');
      await reviewerPage.getByRole('button', { name: r.requestChanges.submit }).click();

      // The reviewer's request-changes commits asynchronously; reload until the
      // supplier's view reflects the new status (avoids racing a static page load).
      await expect(async () => {
        await supplierPage.goto(`/en/supplier/applications/${applicationId}`);
        await expect(
          supplierPage.getByText(s.status.state.CHANGES_REQUESTED, { exact: true }).first(),
        ).toBeVisible({ timeout: 3_000 });
      }).toPass({ timeout: 30_000 });
      await supplierPage.getByLabel(s.companyInfo.registrationNumberLabel).fill('VN-987654');
      await supplierPage.getByRole('button', { name: s.companyInfo.save }).click();
      await supplierPage.getByRole('button', { name: s.review.resubmit }).click();

      // D: a RESUBMITTED application must be re-claimed (→ UNDER_REVIEW) before it
      // can be approved. Reload until the claim control is available (the supplier's
      // resubmit commits asynchronously), claim, then approve.
      await expect(async () => {
        await reviewerPage.goto(`/en/reviewer/${applicationId}`);
        await expect(
          reviewerPage.getByRole('button', { name: r.actions.claim }),
        ).toBeVisible({ timeout: 3_000 });
      }).toPass({ timeout: 30_000 });
      await reviewerPage.getByRole('button', { name: r.actions.claim }).click();
      await reviewerPage.getByRole('button', { name: r.actions.approve }).click();
      await expect(reviewerPage.getByText(r.actions.approved)).toBeVisible({ timeout: 15_000 });
      await reviewerPage.getByRole('button', { name: r.grant.submit }).click();

      // Supplier sees ACTIVE + scope-based verification (not auto-verified).
      await expect(async () => {
        await supplierPage.goto(`/en/supplier/applications/${applicationId}`);
        await expect(
          supplierPage.getByText(s.status.state.APPROVED, { exact: true }).first(),
        ).toBeVisible({ timeout: 3_000 });
      }).toPass({ timeout: 30_000 });
    } finally {
      await supplierCtx.close();
      await reviewerCtx.close();
    }
  });
});

// ---------------------------------------------------------------------------
// Journey E — tenant isolation + reviewer authorization.
// ---------------------------------------------------------------------------
test.describe('isolation and security', () => {
  test('E: a second org cannot access the application; an org OWNER is not a reviewer @smoke', async ({
    browser,
    request,
  }) => {
    test.setTimeout(240_000);

    const owner = uniqueSmokeUser('supplier.e', 'Supplier E Owner');
    const outsider = uniqueSmokeUser('outsider.e', 'Outsider E');

    const ownerCtx = await browser.newContext();
    const outsiderCtx = await browser.newContext();
    const ownerPage = await ownerCtx.newPage();
    const outsiderPage = await outsiderCtx.newPage();

    try {
      await registerVerifyLogin(ownerPage, request, owner);
      await createOrganization(ownerPage, `Iso Optics ${Date.now()}`);
      const applicationId = await startApplication(ownerPage, 'Iso Optics Ltd');

      await registerVerifyLogin(outsiderPage, request, outsider);
      // Concealment: the outsider gets the not-found state, not the workspace.
      await outsiderPage.goto(`/en/supplier/applications/${applicationId}`);
      await expect(
        outsiderPage.getByText(s.application.notFound.title),
      ).toBeVisible();

      // An org OWNER without staff access is forbidden from the reviewer area.
      await outsiderPage.goto(`/en/reviewer/${applicationId}`);
      await expect(outsiderPage.getByText(r.forbidden.title)).toBeVisible();
    } finally {
      await ownerCtx.close();
      await outsiderCtx.close();
    }
  });
});

// ---------------------------------------------------------------------------
// Journey F — RTL critical path (Arabic).
// ---------------------------------------------------------------------------
test.describe('RTL onboarding', () => {
  test('F: onboarding critical path in ar renders dir=rtl without horizontal overflow @smoke', async ({
    page,
    request,
  }) => {
    test.setTimeout(240_000);
    const user = uniqueSmokeUser('supplier.f', 'Supplier F Owner');
    await registerVerifyLogin(page, request, user);
    await createOrganization(page, `RTL Optics ${Date.now()}`, 'ar');

    await startApplication(page, 'RTL Optics Ltd', 'ar');
    // The document direction is right-to-left.
    await expect(page.locator('html')).toHaveAttribute('dir', 'rtl');
    // No horizontal overflow on the workspace.
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
    );
    expect(overflow).toBeTruthy();
  });
});

// ---------------------------------------------------------------------------
// Journey G — CJK routing + original-language preservation (Simplified Chinese).
// ---------------------------------------------------------------------------
test.describe('CJK onboarding', () => {
  test('G: zh-CN routing stores CJK content and preserves the original language @smoke', async ({
    page,
    request,
  }) => {
    test.setTimeout(240_000);
    const user = uniqueSmokeUser('supplier.g', 'Supplier G Owner');
    await registerVerifyLogin(page, request, user);
    await createOrganization(page, `CJK Optics ${Date.now()}`, 'zh-CN');

    await startApplication(page, 'CJK Optics Ltd', 'zh-CN');
    await expect(page).toHaveURL(/\/zh-CN\/supplier\/applications\//);

    // Author CJK original content and confirm it round-trips unchanged.
    const description = '专业光学镜片制造商，提供全套加工服务。';
    await page.getByLabel(s.content.companyDescriptionLabel).fill(description);
    await page.getByRole('button', { name: s.content.save }).first().click();
    await expect(page.getByText(s.content.saved)).toBeVisible({ timeout: 15_000 });
    await page.reload();
    await expect(page.getByLabel(s.content.companyDescriptionLabel)).toHaveValue(description);
  });
});
