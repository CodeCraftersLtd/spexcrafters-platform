import { expect, test, type APIRequestContext } from '@playwright/test';

import en from '../messages/en.json';

/**
 * Walking-skeleton smoke: registration → email verification (via Mailpit) →
 * login → buyer dashboard → logout. Requires the backend API (:8080) and
 * Mailpit (:8025) to be running alongside the web app.
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

async function findVerificationToken(
  request: APIRequestContext,
  email: string,
): Promise<string> {
  let token: string | null = null;

  await expect
    .poll(
      async () => {
        const searchResponse = await request.get(
          `${MAILPIT_URL}/api/v1/search`,
          { params: { query: `to:"${email}"` } },
        );
        if (!searchResponse.ok()) {
          return null;
        }
        const result = (await searchResponse.json()) as MailpitSearchResult;
        const message = result.messages.find((candidate) =>
          candidate.To.some(
            (to) => to.Address.toLowerCase() === email.toLowerCase(),
          ),
        );
        if (!message) {
          return null;
        }

        const messageResponse = await request.get(
          `${MAILPIT_URL}/api/v1/message/${message.ID}`,
        );
        if (!messageResponse.ok()) {
          return null;
        }
        const body = (await messageResponse.json()) as MailpitMessage;
        const content = `${body.Text ?? ''}\n${body.HTML ?? ''}`;
        const match = content.match(/[?&]token=([A-Za-z0-9._~-]+)/);
        token = match?.[1] ?? null;
        return token;
      },
      {
        message: `verification email for ${email} did not arrive in Mailpit`,
        timeout: 30_000,
        intervals: [1_000],
      },
    )
    .not.toBeNull();

  if (!token) {
    throw new Error('verification token not extracted');
  }
  return token;
}

test.describe('auth vertical slice', () => {
  test('register → verify → login → dashboard → logout @smoke', async ({
    page,
    request,
  }) => {
    const uniqueEmail = `e2e.smoke.${Date.now()}.${Math.floor(Math.random() * 1e6)}@spexcrafters.test`;
    const password = 'correct-horse-battery-staple';
    const displayName = 'Smoke Test Buyer';

    // 1. Register.
    await page.goto('/en/auth/register');
    await expect(
      page.getByRole('heading', { name: en.auth.register.title }),
    ).toBeVisible();

    await page.getByLabel(en.auth.register.displayNameLabel).fill(displayName);
    await page.getByLabel(en.auth.register.emailLabel).fill(uniqueEmail);
    await page.getByLabel(en.auth.register.passwordLabel).fill(password);
    await page
      .getByRole('button', { name: en.auth.register.submit })
      .click();

    await expect(page.getByText(en.auth.register.checkEmail.title)).toBeVisible();

    // 2. Pull the verification link from Mailpit.
    const token = await findVerificationToken(request, uniqueEmail);

    // 3. Follow the verification link.
    await page.goto(`/en/auth/verify-email?token=${encodeURIComponent(token)}`);
    await expect(
      page.getByText(en.auth.verifyEmail.successTitle),
    ).toBeVisible({ timeout: 15_000 });

    await page
      .getByRole('link', { name: en.auth.verifyEmail.goToLogin })
      .click();
    await expect(page).toHaveURL(/\/en\/auth\/login/);

    // 4. Log in.
    await page.getByLabel(en.auth.login.emailLabel).fill(uniqueEmail);
    await page.getByLabel(en.auth.login.passwordLabel).fill(password);
    await page.getByRole('button', { name: en.auth.login.submit }).click();

    // 5. Buyer dashboard greets the user by display name.
    await expect(page).toHaveURL(/\/en\/buyer/, { timeout: 15_000 });
    await expect(
      page.getByRole('heading', { name: `Welcome, ${displayName}` }),
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
    await expect(page).toHaveURL(/\/(en|zh-Hans|fr|de)(\/|$)/);
    await expect(
      page.getByRole('heading', { name: en.home.hero.title }),
    ).toBeVisible();
  });
});
