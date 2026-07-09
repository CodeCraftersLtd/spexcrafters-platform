import { NextResponse } from 'next/server';

import { ApiProblemError, type Problem } from '@spexcrafters/api-client';

import type { ClientError, ClientErrorBody, ClientFieldError } from '@/lib/bff';
import { enforceCsrf } from '@/lib/csrf';
import { getSession, refreshIfNeeded, type SessionPayload } from '@/lib/session';

/**
 * Shared BFF plumbing for the organization mutation proxies
 * (src/app/api/orgs/** and src/app/api/invitations/**).
 *
 * Every handler follows the same shape: resolve the session (rotating the
 * token pair when close to expiry), call the typed API client with the access
 * token, and translate any RFC 9457 problem into the browser error envelope
 * `{ error: { code, message, fields? } }`.
 */

/** Uniform 401 for a missing or expired session. */
export function unauthenticatedResponse(): NextResponse<ClientErrorBody> {
  return NextResponse.json(
    { error: { code: 'unauthenticated', message: '' } },
    { status: 401 },
  );
}

/**
 * Resolve a session valid for the upstream call. Returns the ready-to-send
 * 401 response when there is no session or the refresh token was rejected.
 */
export async function requireApiSession(): Promise<SessionPayload | NextResponse<ClientErrorBody>> {
  const session = await getSession();
  if (!session) {
    return unauthenticatedResponse();
  }
  const fresh = await refreshIfNeeded(session);
  if (!fresh) {
    return unauthenticatedResponse();
  }
  return fresh;
}

/**
 * Resolve a session AND enforce CSRF (ADR-018) for an authenticated mutation.
 * Returns the session on success, or a ready-to-send response — 401 when there
 * is no session, 403 when the CSRF guard rejects the request. Use across every
 * org/invitation mutation handler so the guard is wired in exactly one place.
 */
export async function requireApiSessionWithCsrf(
  request: Request,
): Promise<SessionPayload | NextResponse<ClientErrorBody>> {
  const session = await requireApiSession();
  if (isErrorResponse(session)) {
    return session;
  }
  const csrfError = enforceCsrf(request, session);
  if (csrfError) {
    return csrfError;
  }
  return session;
}

export function isErrorResponse(
  value: SessionPayload | NextResponse<ClientErrorBody>,
): value is NextResponse<ClientErrorBody> {
  return value instanceof NextResponse;
}

/** Org-context fallback codes (unlike auth, 403 here means "forbidden"). */
function fallbackCode(status: number): string {
  switch (status) {
    case 401:
      return 'unauthenticated';
    case 403:
      return 'forbidden';
    case 404:
      return 'not-found';
    case 409:
      return 'conflict';
    case 410:
      return 'token-gone';
    case 422:
      return 'validation-failed';
    case 429:
      return 'rate-limited';
    default:
      return 'unexpected';
  }
}

function codeFromProblemType(type: string | undefined): string | null {
  if (!type) {
    return null;
  }
  const segment = type.split('/').filter(Boolean).pop();
  return segment && segment !== 'about:blank' ? segment : null;
}

function problemToClientError(problem: Problem): ClientError {
  const error: ClientError = {
    code: codeFromProblemType(problem.type) ?? fallbackCode(problem.status),
    message: problem.detail ?? problem.title ?? '',
  };
  if (problem.errors && problem.errors.length > 0) {
    const fields: Record<string, ClientFieldError> = {};
    for (const item of problem.errors) {
      fields[item.field] = { code: item.code, message: item.message };
    }
    error.fields = fields;
  }
  return error;
}

/**
 * Translate an error thrown by the API client into the browser error
 * envelope; non-problem errors (network, DNS) become a uniform 502.
 */
export function apiErrorResponse(error: unknown): NextResponse<ClientErrorBody> {
  if (error instanceof ApiProblemError) {
    return NextResponse.json(
      { error: problemToClientError(error.problem) },
      { status: error.problem.status },
    );
  }
  return NextResponse.json(
    { error: { code: 'unexpected', message: '' } },
    { status: 502 },
  );
}
