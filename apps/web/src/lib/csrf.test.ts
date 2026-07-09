// @vitest-environment node
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

// session.ts pulls in next/headers; the guard under test never calls cookies(),
// so an inert stub keeps the import safe in the node test environment.
vi.mock('next/headers', () => ({
  cookies: () => {
    throw new Error('cookies() must not be called from csrf unit tests');
  },
}));

import {
  checkOrigin,
  enforceCsrf,
  enforceUnauthenticatedOrigin,
  requireJsonContentType,
  SAFE_METHODS,
  timingSafeEqualStr,
} from './csrf';
import type { SessionPayload } from './session';

const SITE = 'http://localhost:3000';
const TOKEN = 'sealed-csrf-token-value-abcdef123456';

const session: SessionPayload = {
  accessToken: 'access',
  refreshToken: 'refresh-refresh-refresh',
  accessTokenExpiresAt: Math.floor(Date.now() / 1000) + 600,
  csrfToken: TOKEN,
  user: {
    id: '0195b4f2-7c3a-7000-8000-000000000001',
    email: 'ada@example.com',
    displayName: 'Ada Lovelace',
    locale: 'en',
  },
};

function mutation(headers: Record<string, string>, method = 'POST'): Request {
  return new Request(`${SITE}/api/orgs`, { method, headers });
}

const goodHeaders = {
  origin: SITE,
  'content-type': 'application/json',
  'x-csrf-token': TOKEN,
};

beforeEach(() => {
  process.env.NEXT_PUBLIC_SITE_URL = SITE;
  vi.spyOn(console, 'warn').mockImplementation(() => {});
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('timingSafeEqualStr', () => {
  it('returns true for equal strings', () => {
    expect(timingSafeEqualStr(TOKEN, TOKEN)).toBe(true);
  });

  it('returns false for different equal-length strings', () => {
    expect(timingSafeEqualStr('a'.repeat(10), 'b'.repeat(10))).toBe(false);
  });

  it('returns false (no throw) on length mismatch', () => {
    expect(timingSafeEqualStr('short', 'a-much-longer-value')).toBe(false);
  });
});

describe('checkOrigin', () => {
  it('accepts a matching Origin header', () => {
    expect(checkOrigin(mutation({ origin: SITE })).ok).toBe(true);
  });

  it('rejects a mismatched Origin header', () => {
    expect(checkOrigin(mutation({ origin: 'http://evil.example' })).ok).toBe(false);
  });

  it('falls back to Sec-Fetch-Site when Origin is absent', () => {
    expect(checkOrigin(mutation({ 'sec-fetch-site': 'same-origin' })).ok).toBe(true);
    expect(checkOrigin(mutation({ 'sec-fetch-site': 'none' })).ok).toBe(true);
    expect(checkOrigin(mutation({ 'sec-fetch-site': 'cross-site' })).ok).toBe(false);
  });

  it('rejects when no origin signal is present at all', () => {
    expect(checkOrigin(mutation({})).ok).toBe(false);
  });
});

describe('requireJsonContentType', () => {
  it('accepts application/json (with charset)', () => {
    expect(
      requireJsonContentType(
        mutation({ 'content-type': 'application/json; charset=utf-8' }),
      ),
    ).toBe(true);
  });

  it('rejects form and missing content types', () => {
    expect(
      requireJsonContentType(
        mutation({ 'content-type': 'application/x-www-form-urlencoded' }),
      ),
    ).toBe(false);
    expect(requireJsonContentType(mutation({}))).toBe(false);
  });
});

describe('enforceCsrf (authenticated mutations)', () => {
  it('passes with matching header + origin + json', () => {
    expect(enforceCsrf(mutation(goodHeaders), session)).toBeNull();
  });

  it('bypasses safe methods', () => {
    for (const method of SAFE_METHODS) {
      expect(enforceCsrf(mutation({}, method), session)).toBeNull();
    }
  });

  it('rejects a missing X-CSRF-Token header (403 csrf)', async () => {
    const res = enforceCsrf(
      mutation({ origin: SITE, 'content-type': 'application/json' }),
      session,
    );
    expect(res?.status).toBe(403);
    expect((await res?.json())?.error.code).toBe('csrf');
  });

  it('rejects a wrong token', () => {
    const res = enforceCsrf(
      mutation({ ...goodHeaders, 'x-csrf-token': 'wrong-token' }),
      session,
    );
    expect(res?.status).toBe(403);
  });

  it('rejects a cross-site Origin even with a valid token', () => {
    const res = enforceCsrf(
      mutation({ ...goodHeaders, origin: 'http://evil.example' }),
      session,
    );
    expect(res?.status).toBe(403);
  });

  it('rejects a cross-site Sec-Fetch-Site (no Origin)', () => {
    const res = enforceCsrf(
      mutation({
        'content-type': 'application/json',
        'x-csrf-token': TOKEN,
        'sec-fetch-site': 'cross-site',
      }),
      session,
    );
    expect(res?.status).toBe(403);
  });

  it('rejects a missing JSON content type', () => {
    const res = enforceCsrf(
      mutation({ origin: SITE, 'x-csrf-token': TOKEN }),
      session,
    );
    expect(res?.status).toBe(403);
  });

  it('logs a token-free structured line on failure', () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
    enforceCsrf(mutation({ ...goodHeaders, 'x-csrf-token': 'wrong' }), session);
    expect(warn).toHaveBeenCalledTimes(1);
    const logged = warn.mock.calls[0]?.[0] as string;
    expect(logged).toContain('csrf.validation_failed');
    expect(logged).not.toContain(TOKEN);
    expect(logged).not.toContain('wrong');
  });
});

describe('enforceUnauthenticatedOrigin', () => {
  it('passes with origin + json (no token needed)', () => {
    const req = new Request(`${SITE}/api/auth/login`, {
      method: 'POST',
      headers: { origin: SITE, 'content-type': 'application/json' },
    });
    expect(enforceUnauthenticatedOrigin(req)).toBeNull();
  });

  it('rejects a cross-site origin', () => {
    const req = new Request(`${SITE}/api/auth/login`, {
      method: 'POST',
      headers: { origin: 'http://evil.example', 'content-type': 'application/json' },
    });
    expect(enforceUnauthenticatedOrigin(req)?.status).toBe(403);
  });

  it('rejects a non-JSON content type', () => {
    const req = new Request(`${SITE}/api/auth/login`, {
      method: 'POST',
      headers: { origin: SITE, 'content-type': 'text/plain' },
    });
    expect(enforceUnauthenticatedOrigin(req)?.status).toBe(403);
  });
});
