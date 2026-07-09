import { randomBytes } from 'node:crypto';

import { EncryptJWT, jwtDecrypt } from 'jose';
import { cookies } from 'next/headers';

import type { TokenResponse, UserSummary } from '@spexcrafters/api-client';

import {
  CSRF_COOKIE_NAME,
  SESSION_COOKIE_MAX_AGE,
  SESSION_COOKIE_NAME,
} from '@/lib/cookies';

const JWE_ALG = 'dir';
const JWE_ENC = 'A256GCM';

/** Rotate the access token when it expires within this many seconds. */
const REFRESH_SKEW_SECONDS = 60;

export interface SessionUser {
  id: string;
  email: string;
  displayName: string;
  locale: string;
}

export interface SessionPayload {
  accessToken: string;
  refreshToken: string;
  /** Unix epoch seconds at which the access token expires. */
  accessTokenExpiresAt: number;
  user: SessionUser;
  /**
   * Canonical CSRF synchronizer token (ADR-018). Sealed inside the session and
   * mirrored to the JS-readable `sc_csrf` cookie; the CSRF guard compares the
   * `X-CSRF-Token` header against this value in constant time.
   */
  csrfToken: string;
  [claim: string]: unknown;
}

function decodeSecret(secret: string): Uint8Array {
  const key = Buffer.from(secret, 'base64');
  if (key.length !== 32) {
    throw new Error(
      'SESSION_SECRET must be 32 random bytes, base64-encoded. Generate one with: node -e "console.log(require(\'node:crypto\').randomBytes(32).toString(\'base64\'))"',
    );
  }
  return new Uint8Array(key);
}

function sessionKey(): Uint8Array {
  const secret = process.env.SESSION_SECRET;
  if (!secret) {
    throw new Error('SESSION_SECRET is not set. See apps/web/.env.example.');
  }
  return decodeSecret(secret);
}

/** Encrypt a session payload into a compact JWE. Pure — cookie handling is separate. */
export async function sealSession(
  payload: SessionPayload,
  key: Uint8Array = sessionKey(),
): Promise<string> {
  return new EncryptJWT(payload)
    .setProtectedHeader({ alg: JWE_ALG, enc: JWE_ENC })
    .setIssuedAt()
    .setExpirationTime(`${SESSION_COOKIE_MAX_AGE}s`)
    .encrypt(key);
}

function isSessionPayload(value: unknown): value is SessionPayload {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const candidate = value as Record<string, unknown>;
  const user = candidate.user as Record<string, unknown> | undefined;
  return (
    typeof candidate.accessToken === 'string' &&
    typeof candidate.refreshToken === 'string' &&
    typeof candidate.accessTokenExpiresAt === 'number' &&
    typeof candidate.csrfToken === 'string' &&
    typeof user === 'object' &&
    user !== null &&
    typeof user.id === 'string' &&
    typeof user.email === 'string' &&
    typeof user.displayName === 'string' &&
    typeof user.locale === 'string'
  );
}

/** Decrypt a compact JWE back into a session payload; null on any failure. */
export async function openSession(
  token: string,
  key: Uint8Array = sessionKey(),
): Promise<SessionPayload | null> {
  try {
    const { payload } = await jwtDecrypt(token, key, {
      keyManagementAlgorithms: [JWE_ALG],
      contentEncryptionAlgorithms: [JWE_ENC],
    });
    return isSessionPayload(payload) ? payload : null;
  } catch {
    return null;
  }
}

function toSessionUser(user: UserSummary): SessionUser {
  return {
    id: user.id,
    email: user.email,
    displayName: user.displayName,
    locale: user.locale,
  };
}

function toSessionPayload(tokens: TokenResponse, csrfToken: string): SessionPayload {
  return {
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    accessTokenExpiresAt: Math.floor(Date.now() / 1000) + tokens.expiresIn,
    user: toSessionUser(tokens.user),
    csrfToken,
  };
}

/** Mint a fresh 32-byte CSRF synchronizer token, base64url-encoded. */
function generateCsrfToken(): string {
  return randomBytes(32).toString('base64url');
}

/** Expose the sealed (canonical) CSRF token for the guard. */
export function readSealedCsrf(session: SessionPayload): string {
  return session.csrfToken;
}

async function writeSessionCookie(sealed: string): Promise<void> {
  const store = await cookies();
  store.set(SESSION_COOKIE_NAME, sealed, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    path: '/',
    maxAge: SESSION_COOKIE_MAX_AGE,
  });
}

/**
 * Set the JS-readable CSRF companion cookie (ADR-018). Deliberately NOT
 * HttpOnly — same-origin client code must read it to echo `X-CSRF-Token`.
 * Host-only (no Domain), Secure in production, SameSite=Lax, lifetime pinned
 * to the session.
 */
async function writeCsrfCookie(token: string): Promise<void> {
  const store = await cookies();
  store.set(CSRF_COOKIE_NAME, token, {
    httpOnly: false,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    path: '/',
    maxAge: SESSION_COOKIE_MAX_AGE,
  });
}

/**
 * Persist a freshly issued token pair as the encrypted sc_session cookie and
 * set the matching sc_csrf cookie. Callable from Route Handlers and Server
 * Actions (cookie mutation context).
 *
 * Pass `existingCsrf` to PRESERVE the current session's CSRF token across an
 * access-token refresh (session identity is continuous — the token must not
 * change or concurrent tabs would diverge). Omit it on a fresh login to mint a
 * new token (rotation on new session).
 */
export async function createSession(
  tokens: TokenResponse,
  existingCsrf?: string,
): Promise<SessionPayload> {
  const csrfToken = existingCsrf ?? generateCsrfToken();
  const payload = toSessionPayload(tokens, csrfToken);
  await writeSessionCookie(await sealSession(payload));
  await writeCsrfCookie(csrfToken);
  return payload;
}

/** Read and decrypt the current session cookie; null when absent or invalid. */
export async function getSession(): Promise<SessionPayload | null> {
  const store = await cookies();
  const cookie = store.get(SESSION_COOKIE_NAME);
  if (!cookie?.value) {
    return null;
  }
  return openSession(cookie.value);
}

/** Remove both the session cookie and the CSRF companion, matching attributes. */
export async function destroySession(): Promise<void> {
  const store = await cookies();
  store.delete({ name: SESSION_COOKIE_NAME, path: '/' });
  store.delete({ name: CSRF_COOKIE_NAME, path: '/' });
}

const API_BASE_URL = process.env.API_BASE_URL ?? 'http://localhost:8080/api/v1';

/**
 * Return a session whose access token is valid for at least another
 * REFRESH_SKEW_SECONDS, exchanging the refresh token and rotating the cookie
 * when necessary. Must be called from a cookie-mutation context (Route
 * Handler / Server Action); Server Components should use getSession() only.
 * Returns null (and clears the cookie) when the refresh token is rejected.
 */
export async function refreshIfNeeded(
  session: SessionPayload,
): Promise<SessionPayload | null> {
  const now = Math.floor(Date.now() / 1000);
  if (session.accessTokenExpiresAt - now > REFRESH_SKEW_SECONDS) {
    return session;
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ refreshToken: session.refreshToken }),
      cache: 'no-store',
    });
  } catch {
    // Upstream unreachable: keep the existing session rather than logging
    // the user out over a transient network failure.
    return session;
  }

  if (!response.ok) {
    // Grace-window loser: a concurrent request (another tab) already rotated
    // this token. That is NOT theft — keep the session as-is. The current
    // access token is still valid (we only refresh within REFRESH_SKEW of
    // expiry), so this request proceeds; the winning response has already
    // placed the rotated sc_session cookie in the shared browser jar for the
    // tab's next request. A distinct problem type keeps a genuine auth
    // failure tearing the session down.
    if (await isConcurrentRefresh(response)) {
      return session;
    }
    await destroySession();
    return null;
  }

  const tokens = (await response.json()) as TokenResponse;
  // Preserve the CSRF token across refresh: the session identity is continuous,
  // so the token must not change (multi-tab safety).
  return createSession(tokens, session.csrfToken);
}

/** True when a failed refresh is a benign concurrent-refresh race, not a rejection. */
async function isConcurrentRefresh(response: Response): Promise<boolean> {
  if (response.status !== 401) {
    return false;
  }
  try {
    const problem = (await response.clone().json()) as { type?: string };
    return typeof problem.type === 'string' && problem.type.endsWith('/problems/concurrent-refresh');
  } catch {
    return false;
  }
}
