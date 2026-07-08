import { expect, type APIRequestContext, type Page } from '@playwright/test';

import en from '../messages/en.json';

/**
 * Shared smoke-test helpers: Mailpit polling and the register → verify →
 * login journey. The suites drive the real stack — this app (next dev), the
 * Spring Boot API on :8080, and Mailpit on :8025.
 */

const MAILPIT_URL = process.env.MAILPIT_URL ?? 'http://localhost:8025';

interface MailpitAddress {
  Address: string;
}

interface MailpitMessageSummary {
  ID: string;
  To: MailpitAddress[];
}

interface MailpitSearchResult {
  messages: MailpitMessageSummary[];
}

interface MailpitMessage {
  Text: string;
  HTML: string;
}

/**
 * Poll Mailpit for an email to `email` whose body matches `tokenPattern`
 * (first capture group = token). Emails of other kinds addressed to the same
 * mailbox (e.g. a verification email next to an invitation email) are skipped
 * because their bodies do not match the pattern.
 */
export async function findEmailToken(
  request: APIRequestContext,
  email: string,
  tokenPattern: RegExp,
  description: string,
): Promise<string> {
  let token: string | null = null;

  await expect
    .poll(
      async () => {
        const searchResponse = await request.get(`${MAILPIT_URL}/api/v1/search`, {
          params: { query: `to:"${email}"` },
        });
        if (!searchResponse.ok()) {
          return null;
        }
        const result = (await searchResponse.json()) as MailpitSearchResult;
        const candidates = result.messages.filter((candidate) =>
          candidate.To.some(
            (to) => to.Address.toLowerCase() === email.toLowerCase(),
          ),
        );
        for (const message of candidates) {
          const messageResponse = await request.get(
            `${MAILPIT_URL}/api/v1/message/${message.ID}`,
          );
          if (!messageResponse.ok()) {
            continue;
          }
          const body = (await messageResponse.json()) as MailpitMessage;
          const content = `${body.Text ?? ''}\n${body.HTML ?? ''}`;
          const match = content.match(tokenPattern);
          if (match?.[1]) {
            token = match[1];
            return token;
          }
        }
        return null;
      },
      {
        message: `${description} for ${email} did not arrive in Mailpit`,
        timeout: 30_000,
        intervals: [1_000],
      },
    )
    .not.toBeNull();

  if (!token) {
    throw new Error(`${description} token not extracted`);
  }
  return token;
}

/**
 * Pull the account-verification token from the registration email. The
 * pattern is intentionally loose (any token= parameter): registration is
 * always the first email a test identity receives, so there is no ambiguity —
 * and the auth suite predates the stricter link-path matching.
 */
export async function findVerificationToken(
  request: APIRequestContext,
  email: string,
): Promise<string> {
  return findEmailToken(
    request,
    email,
    /[?&]token=([A-Za-z0-9._~-]+)/,
    'verification email',
  );
}

/** Pull the invitation token from the organization-invitation email. */
export async function findInvitationToken(
  request: APIRequestContext,
  email: string,
): Promise<string> {
  return findEmailToken(
    request,
    email,
    /invitations\/accept\?token=([A-Za-z0-9._~-]+)/,
    'invitation email',
  );
}

export interface SmokeUser {
  email: string;
  password: string;
  displayName: string;
}

/** Unique test identity; passwords satisfy the policy (≥12 chars incl. a digit). */
export function uniqueSmokeUser(prefix: string, displayName: string): SmokeUser {
  return {
    email: `e2e.${prefix}.${Date.now()}.${Math.floor(Math.random() * 1e6)}@spexcrafters.test`,
    password: 'correct-horse-battery-staple-9',
    displayName,
  };
}

/**
 * Full register → verify (via Mailpit) → login journey, ending on the buyer
 * dashboard. Waits for hydration before interacting: a pre-hydration click
 * would trigger a native (non-RHF) submit instead of the client handler.
 */
export async function registerVerifyLogin(
  page: Page,
  request: APIRequestContext,
  user: SmokeUser,
): Promise<void> {
  // 1. Register.
  await page.goto('/en/auth/register');
  await page.waitForLoadState('networkidle');
  await expect(
    page.getByRole('heading', { name: en.auth.register.title }),
  ).toBeVisible();

  await page.getByLabel(en.auth.register.displayNameLabel).fill(user.displayName);
  await page.getByLabel(en.auth.register.emailLabel).fill(user.email);
  await page.getByLabel(en.auth.register.passwordLabel).fill(user.password);
  await page.getByRole('button', { name: en.auth.register.submit }).click();

  await expect(page.getByText(en.auth.register.checkEmail.title)).toBeVisible();

  // 2. Pull the verification link from Mailpit and follow it.
  const token = await findVerificationToken(request, user.email);
  await page.goto(`/en/auth/verify-email?token=${encodeURIComponent(token)}`);
  await expect(page.getByText(en.auth.verifyEmail.successTitle)).toBeVisible({
    timeout: 15_000,
  });

  // 3. Log in.
  await page.getByRole('link', { name: en.auth.verifyEmail.goToLogin }).click();
  await expect(page).toHaveURL(/\/en\/auth\/login/);
  await page.waitForLoadState('networkidle');

  await page.getByLabel(en.auth.login.emailLabel).fill(user.email);
  await page.getByLabel(en.auth.login.passwordLabel).fill(user.password);
  await page.getByRole('button', { name: en.auth.login.submit }).click();

  // 4. Buyer dashboard greets the user by display name.
  await expect(page).toHaveURL(/\/en\/buyer/, { timeout: 15_000 });
  await expect(
    page.getByRole('heading', { name: `Welcome, ${user.displayName}` }),
  ).toBeVisible();
}
