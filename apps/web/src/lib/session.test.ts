// @vitest-environment node
import { describe, expect, it, vi } from 'vitest';

// session.ts imports next/headers for its cookie helpers; the pure JWE
// functions under test never call it, so a stub keeps the import inert.
vi.mock('next/headers', () => ({
  cookies: () => {
    throw new Error('cookies() must not be called from unit tests');
  },
}));

import { openSession, sealSession, type SessionPayload } from './session';

const KEY = new Uint8Array(32).fill(7);
const OTHER_KEY = new Uint8Array(32).fill(9);

const payload: SessionPayload = {
  accessToken: 'access-token-value',
  refreshToken: 'refresh-token-value-refresh-token-value',
  accessTokenExpiresAt: Math.floor(Date.now() / 1000) + 600,
  csrfToken: 'csrf-token-value-csrf-token-value-abc',
  user: {
    id: '0195b4f2-7c3a-7000-8000-000000000001',
    email: 'ada@example.com',
    displayName: 'Ada Lovelace',
    locale: 'en',
  },
};

describe('session JWE round trip', () => {
  it('seals and opens a session payload with a fixed key', async () => {
    const token = await sealSession(payload, KEY);
    expect(token.split('.')).toHaveLength(5); // compact JWE

    const opened = await openSession(token, KEY);
    expect(opened).not.toBeNull();
    expect(opened?.accessToken).toBe(payload.accessToken);
    expect(opened?.refreshToken).toBe(payload.refreshToken);
    expect(opened?.accessTokenExpiresAt).toBe(payload.accessTokenExpiresAt);
    expect(opened?.user).toEqual(payload.user);
  });

  it('seals and round-trips the CSRF token (ADR-018 canonical value)', async () => {
    const token = await sealSession(payload, KEY);
    // The token is the canonical CSRF value and must never appear in cleartext.
    expect(token).not.toContain(payload.csrfToken);

    const opened = await openSession(token, KEY);
    expect(opened?.csrfToken).toBe(payload.csrfToken);
  });

  it('rejects a payload with no CSRF token (shape guard)', async () => {
    const invalid = {
      accessToken: payload.accessToken,
      refreshToken: payload.refreshToken,
      accessTokenExpiresAt: payload.accessTokenExpiresAt,
      user: payload.user,
    } as unknown as SessionPayload;
    const token = await sealSession(invalid, KEY);
    expect(await openSession(token, KEY)).toBeNull();
  });

  it('produces an unreadable (encrypted) compact token', async () => {
    const token = await sealSession(payload, KEY);
    expect(token).not.toContain('ada@example.com');
    expect(token).not.toContain('access-token-value');
  });

  it('returns null for a token sealed with a different key', async () => {
    const token = await sealSession(payload, KEY);
    expect(await openSession(token, OTHER_KEY)).toBeNull();
  });

  it('returns null for a tampered token', async () => {
    const token = await sealSession(payload, KEY);
    const tampered = `${token.slice(0, -4)}AAAA`;
    expect(await openSession(tampered, KEY)).toBeNull();
  });

  it('returns null for garbage input', async () => {
    expect(await openSession('not-a-jwe', KEY)).toBeNull();
    expect(await openSession('', KEY)).toBeNull();
  });

  it('returns null when the decrypted payload is not a session shape', async () => {
    // Seal a structurally invalid payload by casting; openSession must
    // reject it at the shape guard rather than returning a partial object.
    const invalid = { hello: 'world' } as unknown as SessionPayload;
    const token = await sealSession(invalid, KEY);
    expect(await openSession(token, KEY)).toBeNull();
  });
});
