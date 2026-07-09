import { expect, test, type Page } from '@playwright/test';

import { en } from './messages';

import { findInvitationToken, registerVerifyLogin, uniqueSmokeUser } from './helpers';

/**
 * Phase-5 organizations smoke: creation → OWNER workspace → cross-tenant
 * concealment (both directions) → invitation over Mailpit → MEMBER accept →
 * capability enforcement → session guard. Self-contained: registers its own
 * users. Requires the backend API (:8080) and Mailpit (:8025).
 */

const orgCopy = en.organizations;
const workspaceCopy = orgCopy.workspace;

async function createOrganization(page: Page, name: string): Promise<string> {
  await page.goto('/en/organizations/new');
  await page.waitForLoadState('networkidle');
  await expect(
    page.getByRole('heading', { name: orgCopy.create.title }),
  ).toBeVisible();

  await page.getByLabel(orgCopy.create.nameLabel).fill(name);
  await page.getByLabel(orgCopy.create.typeLabel).selectOption('BUYER');
  await page.getByRole('button', { name: orgCopy.create.submit }).click();

  // Success redirects to the new workspace: /en/organizations/{uuid}. Match a
  // UUID specifically — the literal ".../organizations/new" the form submits
  // from also matches a generic [^/]+ segment, so a loose pattern would return
  // before the redirect and capture "new" as the id.
  const workspaceUrl =
    /\/en\/organizations\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})$/;
  await page.waitForURL(workspaceUrl, { timeout: 15_000 });
  const match = page.url().match(workspaceUrl);
  if (!match?.[1]) {
    throw new Error(`could not extract organization id from ${page.url()}`);
  }
  await expect(page.getByRole('heading', { name })).toBeVisible();
  return match[1];
}

async function expectWorkspaceNotFound(page: Page, organizationId: string) {
  await page.goto(`/en/organizations/${organizationId}`);
  await expect(
    page.getByRole('heading', { name: workspaceCopy.notFound.title }),
  ).toBeVisible();
  await expect(page.getByText(workspaceCopy.notFound.body)).toBeVisible();
}

test.describe('organizations vertical slice', () => {
  test('create → concealment → invite → accept → capabilities → guard @smoke', async ({
    browser,
    request,
  }) => {
    test.setTimeout(240_000);

    const userA = uniqueSmokeUser('org.owner', 'Org Smoke Owner');
    const userB = uniqueSmokeUser('org.member', 'Org Smoke Member');
    const orgAName = `Smoke Org A ${Date.now()}`;
    const orgBName = `Smoke Org B ${Date.now()}`;

    const contextA = await browser.newContext();
    const contextB = await browser.newContext();
    const pageA = await contextA.newPage();
    const pageB = await contextB.newPage();

    try {
      // Two independent identities in two isolated browser contexts.
      await registerVerifyLogin(pageA, request, userA);
      await registerVerifyLogin(pageB, request, userB);

      // A starts with an empty organizations list.
      await pageA.goto('/en/organizations');
      await expect(pageA.getByText(orgCopy.list.empty.title)).toBeVisible();

      // A creates Organization A and lands in its workspace as OWNER.
      const orgAId = await createOrganization(pageA, orgAName);
      await expect(
        pageA.getByText(`${workspaceCopy.yourRoleLabel}: ${orgCopy.roles.OWNER}`),
      ).toBeVisible();
      await expect(
        pageA.getByText(`${userA.displayName} (${workspaceCopy.members.youLabel})`),
      ).toBeVisible();

      // Concealment, direction 1: B gets the not-found state for Org A.
      await expectWorkspaceNotFound(pageB, orgAId);

      // B creates Organization B; concealment, direction 2: A cannot see it.
      const orgBId = await createOrganization(pageB, orgBName);
      await expectWorkspaceNotFound(pageA, orgBId);

      // A invites B's email as MEMBER.
      await pageA.goto(`/en/organizations/${orgAId}`);
      await pageA.waitForLoadState('networkidle');
      await pageA
        .getByLabel(workspaceCopy.invitations.emailLabel)
        .fill(userB.email);
      await pageA
        .getByLabel(workspaceCopy.invitations.roleLabel)
        .selectOption('MEMBER');
      await pageA
        .getByRole('button', { name: workspaceCopy.invitations.submit })
        .click();
      await expect(pageA.getByText(workspaceCopy.invitations.sent)).toBeVisible({
        timeout: 15_000,
      });
      await expect(pageA.getByText(userB.email)).toBeVisible();

      // B pulls the invitation email from Mailpit and accepts the token.
      const invitationToken = await findInvitationToken(request, userB.email);
      await pageB.goto(
        `/en/invitations/accept?token=${encodeURIComponent(invitationToken)}`,
      );
      await expect(
        pageB.getByText(en.invitations.accept.successTitle),
      ).toBeVisible({ timeout: 15_000 });

      // B opens Org A's workspace as MEMBER.
      await pageB
        .getByRole('link', { name: `Open ${orgAName}` })
        .click();
      await expect(pageB).toHaveURL(new RegExp(`/en/organizations/${orgAId}$`));
      await pageB.waitForLoadState('networkidle');
      await expect(
        pageB.getByRole('heading', { name: orgAName }),
      ).toBeVisible();
      await expect(
        pageB.getByText(`${workspaceCopy.yourRoleLabel}: ${orgCopy.roles.MEMBER}`),
      ).toBeVisible();

      // Capability enforcement: a MEMBER sees no invite controls.
      await expect(
        pageB.getByRole('button', { name: workspaceCopy.invitations.submit }),
      ).toHaveCount(0);
      await expect(
        pageB.getByLabel(workspaceCopy.invitations.emailLabel),
      ).toHaveCount(0);

      // A's members list now shows B. Exact match: as OWNER, A also renders a
      // role-select whose accessible label ("Role for <name>") contains the
      // display name, so a loose text match resolves to two elements.
      await pageA.goto(`/en/organizations/${orgAId}`);
      await expect(
        pageA.getByText(userB.displayName, { exact: true }),
      ).toBeVisible();

      // Logout A → the organizations area redirects to login.
      await pageA.getByRole('button', { name: orgCopy.logout }).click();
      await expect(pageA).toHaveURL(/\/en$/, { timeout: 15_000 });
      await pageA.goto('/en/organizations');
      await expect(pageA).toHaveURL(/\/en\/auth\/login\?returnTo=/);
    } finally {
      await contextA.close();
      await contextB.close();
    }
  });
});
