import { timingSafeEqual } from 'node:crypto';

import { NextResponse } from 'next/server';

import type { ClientErrorBody } from '@/lib/bff';
import { readSealedCsrf, type SessionPayload } from '@/lib/session';

/**
 * CSRF guard for the BFF cookie-authenticated surface (ADR-018).
 *
 * The canonical token is sealed inside the JWE session and mirrored to the
 * JS-readable `sc_csrf` cookie. Same-origin client code echoes it in
 * `X-CSRF-Token`; this module compares header vs sealed value in constant time
 * and layers Origin / Sec-Fetch-Site / JSON-content-type checks. No single
 * header is ever the sole defense: authenticated mutations always run the token
 * comparison in addition to the origin layer.
 */

/** Methods that never mutate state and therefore bypass the guard. */
export const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

const CSRF_HEADER = 'x-csrf-token';

/** Structured web-tier check result; the reason is logged, never the token. */
interface CheckResult {
  ok: boolean;
  reason: string;
}

/** Constant-time string comparison; false (no throw) on length mismatch. */
export function timingSafeEqualStr(a: string, b: string): boolean {
  const bufA = Buffer.from(a, 'utf8');
  const bufB = Buffer.from(b, 'utf8');
  // A length mismatch is itself a non-match; comparing unequal-length buffers
  // with timingSafeEqual throws, so short-circuit without leaking which is
  // longer (the lengths are not secret — the token content is).
  if (bufA.length !== bufB.length) {
    return false;
  }
  return timingSafeEqual(bufA, bufB);
}

/** The site's canonical origin, from NEXT_PUBLIC_SITE_URL or the request URL. */
function siteOrigin(request: Request): string {
  const configured = process.env.NEXT_PUBLIC_SITE_URL;
  if (configured) {
    try {
      return new URL(configured).origin;
    } catch {
      // Fall through to the request origin when misconfigured.
    }
  }
  return new URL(request.url).origin;
}

function routeOf(request: Request): string {
  try {
    return new URL(request.url).pathname;
  } catch {
    return '(unknown)';
  }
}

/**
 * Origin layer (defense in depth). Prefers the `Origin` header, then
 * `Sec-Fetch-Site`, then `Referer` as a last-resort corroborator. When none of
 * the three is present, a mutation is rejected (documented last resort — a
 * same-origin browser fetch always sends at least one).
 */
export function checkOrigin(request: Request): CheckResult {
  const expected = siteOrigin(request);

  const origin = request.headers.get('origin');
  if (origin) {
    return origin === expected
      ? { ok: true, reason: 'origin-match' }
      : { ok: false, reason: 'origin-mismatch' };
  }

  const secFetchSite = request.headers.get('sec-fetch-site');
  if (secFetchSite) {
    return secFetchSite === 'same-origin' || secFetchSite === 'none'
      ? { ok: true, reason: 'sec-fetch-site-ok' }
      : { ok: false, reason: 'sec-fetch-site-cross' };
  }

  const referer = request.headers.get('referer');
  if (referer) {
    try {
      return new URL(referer).origin === expected
        ? { ok: true, reason: 'referer-match' }
        : { ok: false, reason: 'referer-mismatch' };
    } catch {
      return { ok: false, reason: 'referer-unparseable' };
    }
  }

  return { ok: false, reason: 'origin-absent' };
}

/** BFF mutations must be `application/json` (excludes cross-site HTML forms). */
export function requireJsonContentType(request: Request): boolean {
  const contentType = request.headers.get('content-type');
  return (
    contentType !== null &&
    contentType.trim().toLowerCase().startsWith('application/json')
  );
}

/** Structured, token-free log line for a rejected mutation. */
function logFailure(route: string, reason: string): void {
  // Never log the token value — only the route and the coarse reason.
  console.warn(
    JSON.stringify({ event: 'csrf.validation_failed', route, reason }),
  );
}

function csrfForbidden(): NextResponse<ClientErrorBody> {
  return NextResponse.json(
    { error: { code: 'csrf', message: '' } },
    { status: 403 },
  );
}

/**
 * Enforce CSRF protection for an authenticated mutation. Returns a ready-to-send
 * 403 response on any failure, or null when the request passes (safe methods
 * always pass). Runs, in order: origin layer, JSON content-type, header
 * presence, constant-time token comparison against the sealed session value.
 */
export function enforceCsrf(
  request: Request,
  session: SessionPayload,
): NextResponse<ClientErrorBody> | null {
  if (SAFE_METHODS.has(request.method)) {
    return null;
  }
  const route = routeOf(request);

  const origin = checkOrigin(request);
  if (!origin.ok) {
    logFailure(route, origin.reason);
    return csrfForbidden();
  }

  if (!requireJsonContentType(request)) {
    logFailure(route, 'content-type');
    return csrfForbidden();
  }

  const header = request.headers.get(CSRF_HEADER);
  if (!header) {
    logFailure(route, 'header-missing');
    return csrfForbidden();
  }

  if (!timingSafeEqualStr(header, readSealedCsrf(session))) {
    logFailure(route, 'token-mismatch');
    return csrfForbidden();
  }

  return null;
}

/**
 * Enforce the origin + JSON-content-type layer for unauthenticated
 * state-changers (`/api/auth/login|register|verify-email|resend-verification`).
 * No token exists pre-session, so the layered origin checks close the login-CSRF
 * vector. Returns a 403 on failure, null on pass.
 */
export function enforceUnauthenticatedOrigin(
  request: Request,
): NextResponse<ClientErrorBody> | null {
  if (SAFE_METHODS.has(request.method)) {
    return null;
  }
  const route = routeOf(request);

  const origin = checkOrigin(request);
  if (!origin.ok) {
    logFailure(route, origin.reason);
    return csrfForbidden();
  }

  if (!requireJsonContentType(request)) {
    logFailure(route, 'content-type');
    return csrfForbidden();
  }

  return null;
}
