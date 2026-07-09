// @vitest-environment node
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { CSRF_COOKIE_NAME, SESSION_COOKIE_NAME } from './cookies';

/**
 * Exercises createSession / refreshIfNeeded / destroySession against an
 * in-memory cookie store, focusing on the ADR-018 CSRF lifecycle: minting on a
 * fresh session, preservation across an access-token refresh (multi-tab
 * safety), and deletion of both cookies on logout.
 */

// A 32-byte base64 secret so sessionKey() accepts it.
process.env.SESSION_SECRET = Buffer.alloc(32, 7).toString('base64');

interface StoredCookie {
  value: string;
  options: Record<string, unknown>;
}

const store = new Map<string, StoredCookie>();

vi.mock('next/headers', () => ({
  cookies: () =>
    Promise.resolve({
      get: (name: string) => {
        const entry = store.get(name);
        return entry ? { name, value: entry.value } : undefined;
      },
      set: (name: string, value: string, options: Record<string, unknown>) => {
        store.set(name, { value, options });
      },
      delete: (arg: string | { name: string; path?: string }) => {
        const name = typeof arg === 'string' ? arg : arg.name;
        store.delete(name);
      },
    }),
}));

import {
  createSession,
  destroySession,
  getSession,
  refreshIfNeeded,
} from './session';

const now = () => Math.floor(Date.now() / 1000);

function tokenResponse(overrides: Record<string, unknown> = {}) {
  return {
    accessToken: 'access-token-value',
    tokenType: 'Bearer' as const,
    refreshToken: 'refresh-token-value-refresh-token-value',
    expiresIn: 600,
    user: {
      id: '0195b4f2-7c3a-7000-8000-000000000001',
      email: 'ada@example.com',
      displayName: 'Ada Lovelace',
      locale: 'en',
      emailVerified: true,
      createdAt: '2026-01-01T00:00:00Z',
    },
    ...overrides,
  };
}

beforeEach(() => {
  store.clear();
});

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe('createSession CSRF issuance', () => {
  it('mints a CSRF token and sets a non-HttpOnly sc_csrf cookie', async () => {
    const session = await createSession(tokenResponse());

    expect(session.csrfToken).toMatch(/^[A-Za-z0-9_-]+$/); // base64url
    const csrf = store.get(CSRF_COOKIE_NAME);
    expect(csrf?.value).toBe(session.csrfToken);
    // Deliberately readable per ADR-018, but Lax + host-only.
    expect(csrf?.options.httpOnly).toBe(false);
    expect(csrf?.options.sameSite).toBe('lax');
    expect(csrf?.options.path).toBe('/');
    expect(csrf?.options.domain).toBeUndefined();

    // The credential cookie stays HttpOnly.
    expect(store.get(SESSION_COOKIE_NAME)?.options.httpOnly).toBe(true);
  });

  it('reuses an existing CSRF token when one is passed (refresh path)', async () => {
    const session = await createSession(tokenResponse(), 'preserved-csrf-token');
    expect(session.csrfToken).toBe('preserved-csrf-token');
    expect(store.get(CSRF_COOKIE_NAME)?.value).toBe('preserved-csrf-token');
  });

  it('mints a distinct token per fresh login (rotation)', async () => {
    const a = await createSession(tokenResponse());
    const b = await createSession(tokenResponse());
    expect(a.csrfToken).not.toBe(b.csrfToken);
  });
});

describe('refreshIfNeeded CSRF preservation', () => {
  it('preserves the CSRF token across an access-token rotation', async () => {
    const original = await createSession(tokenResponse({ expiresIn: 0 }));

    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify(
            tokenResponse({
              accessToken: 'rotated-access',
              refreshToken: 'rotated-refresh-rotated-refresh-value',
            }),
          ),
          { status: 200, headers: { 'content-type': 'application/json' } },
        ),
      ),
    );

    const refreshed = await refreshIfNeeded({
      ...original,
      accessTokenExpiresAt: now(), // due for refresh
    });

    expect(refreshed).not.toBeNull();
    expect(refreshed?.accessToken).toBe('rotated-access');
    // The token — and the cookie — must be identical across refresh.
    expect(refreshed?.csrfToken).toBe(original.csrfToken);
    expect(store.get(CSRF_COOKIE_NAME)?.value).toBe(original.csrfToken);
  });
});

describe('destroySession', () => {
  it('deletes both sc_session and sc_csrf', async () => {
    await createSession(tokenResponse());
    expect(store.has(SESSION_COOKIE_NAME)).toBe(true);
    expect(store.has(CSRF_COOKIE_NAME)).toBe(true);

    await destroySession();
    expect(store.has(SESSION_COOKIE_NAME)).toBe(false);
    expect(store.has(CSRF_COOKIE_NAME)).toBe(false);
    expect(await getSession()).toBeNull();
  });
});
