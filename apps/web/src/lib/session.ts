import { EncryptJWT, jwtDecrypt } from 'jose';
import { cookies } from 'next/headers';

import type { TokenResponse, UserSummary } from '@spexcrafters/api-client';

import { SESSION_COOKIE_MAX_AGE, SESSION_COOKIE_NAME } from '@/lib/cookies';

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

function toSessionPayload(tokens: TokenResponse): SessionPayload {
  return {
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    accessTokenExpiresAt: Math.floor(Date.now() / 1000) + tokens.expiresIn,
    user: toSessionUser(tokens.user),
  };
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
 * Persist a freshly issued token pair as the encrypted sc_session cookie.
 * Callable from Route Handlers and Server Actions (cookie mutation context).
 */
export async function createSession(tokens: TokenResponse): Promise<SessionPayload> {
  const payload = toSessionPayload(tokens);
  await writeSessionCookie(await sealSession(payload));
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

/** Remove the session cookie. */
export async function destroySession(): Promise<void> {
  const store = await cookies();
  store.delete(SESSION_COOKIE_NAME);
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
    await destroySession();
    return null;
  }

  const tokens = (await response.json()) as TokenResponse;
  return createSession(tokens);
}
