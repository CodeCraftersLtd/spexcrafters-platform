import { NextResponse } from 'next/server';

import type { Problem } from '@spexcrafters/api-client';

const API_BASE_URL = process.env.API_BASE_URL ?? 'http://localhost:8080/api/v1';

/** Field-level error surfaced to the browser. */
export interface ClientFieldError {
  code: string;
  message: string;
}

/** Uniform error envelope the BFF returns to the browser. */
export interface ClientError {
  code: string;
  message: string;
  fields?: Record<string, ClientFieldError>;
}

export interface ClientErrorBody {
  error: ClientError;
}

/** POST a JSON body to the upstream SpexCrafters API. */
export async function apiFetch(path: string, body: unknown): Promise<Response> {
  return fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      accept: 'application/json, application/problem+json',
    },
    body: JSON.stringify(body),
    cache: 'no-store',
  });
}

function fallbackCode(status: number): string {
  switch (status) {
    case 401:
      return 'authentication-failed';
    case 403:
      return 'email-not-verified';
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

/**
 * Translate an upstream RFC 9457 problem+json response into the client error
 * envelope `{ error: { code, message, fields? } }`, preserving the status and
 * Retry-After where present.
 */
export async function problemToResponse(
  upstream: Response,
): Promise<NextResponse<ClientErrorBody>> {
  let problem: Partial<Problem> | null = null;
  try {
    problem = (await upstream.json()) as Partial<Problem>;
  } catch {
    problem = null;
  }

  const code = codeFromProblemType(problem?.type) ?? fallbackCode(upstream.status);
  const error: ClientError = {
    code,
    message: problem?.detail ?? problem?.title ?? '',
  };

  if (problem?.errors && problem.errors.length > 0) {
    const fields: Record<string, ClientFieldError> = {};
    for (const item of problem.errors) {
      fields[item.field] = { code: item.code, message: item.message };
    }
    error.fields = fields;
  }

  const headers = new Headers();
  const retryAfter = upstream.headers.get('retry-after');
  if (retryAfter) {
    headers.set('retry-after', retryAfter);
  }

  return NextResponse.json({ error }, { status: upstream.status, headers });
}

/** Uniform response when the upstream API is unreachable. */
export function upstreamUnavailable(): NextResponse<ClientErrorBody> {
  return NextResponse.json(
    { error: { code: 'unexpected', message: '' } },
    { status: 502 },
  );
}

/** Uniform response for a syntactically invalid request body. */
export function invalidRequestBody(): NextResponse<ClientErrorBody> {
  return NextResponse.json(
    { error: { code: 'validation-failed', message: '' } },
    { status: 400 },
  );
}

/** Parse a JSON request body, returning null when absent or malformed. */
export async function readJsonBody<T>(request: Request): Promise<T | null> {
  try {
    return (await request.json()) as T;
  } catch {
    return null;
  }
}
