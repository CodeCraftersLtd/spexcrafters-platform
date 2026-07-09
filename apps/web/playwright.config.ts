import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end configuration. The smoke suite drives the real stack:
 * this app (next dev), the Spring Boot API on :8080, and Mailpit on :8025.
 * Override the app origin with PLAYWRIGHT_BASE_URL.
 */
const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:3000';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  // Registration hashes with Argon2id (deliberately CPU-heavy); many parallel
  // workers running concurrent registrations starve each other and blow assertion
  // timeouts. Cap at 2 — the journeys are long and stateful, so throughput is not
  // the constraint. Correctness under contention is.
  workers: 2,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? [['html'], ['github']] : [['list']],
  // Default per-assertion timeout — generous enough to ride out slow Argon2id
  // under load; explicit per-call timeouts still override where needed.
  expect: { timeout: 15_000 },
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'pnpm dev',
    url: baseURL,
    reuseExistingServer: true,
    timeout: 120_000,
  },
});
