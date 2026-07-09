import { CSRF_COOKIE_NAME } from '@/lib/cookies';

/**
 * Client-side CSRF transport (ADR-018). The single place that reads the CSRF
 * cookie — no per-component cookie logic.
 *
 * Credential-exposure note: this reads only `sc_csrf`, which is NOT a
 * credential. The `sc_session` JWE cookie is HttpOnly and can never be read
 * from JavaScript (nor should it be). `sc_csrf` authorizes nothing on its own —
 * replaying it without the HttpOnly session cookie is useless; its sole purpose
 * is to prove the request came from a same-origin context able to read our
 * cookies. Nothing here (or anywhere client-side) touches localStorage /
 * sessionStorage.
 */

/** Read the JS-readable `sc_csrf` cookie value, or null when absent. */
function readCsrfCookie(): string | null {
  if (typeof document === 'undefined') {
    return null;
  }
  const prefix = `${CSRF_COOKIE_NAME}=`;
  for (const part of document.cookie.split('; ')) {
    if (part.startsWith(prefix)) {
      return decodeURIComponent(part.slice(prefix.length));
    }
  }
  return null;
}

/**
 * `fetch` wrapper that attaches the `X-CSRF-Token` header from the `sc_csrf`
 * cookie (when present) and pins same-origin credentials. Existing headers on
 * `init` (e.g. content-type) are preserved.
 */
export function csrfFetch(
  input: RequestInfo | URL,
  init: RequestInit = {},
): Promise<Response> {
  const headers = new Headers(init.headers);
  const token = readCsrfCookie();
  if (token) {
    headers.set('X-CSRF-Token', token);
  }
  return fetch(input, { ...init, headers, credentials: 'same-origin' });
}

/**
 * Convenience for the app's JSON mutations: always sends
 * `Content-Type: application/json` (required by the BFF CSRF guard, even for
 * bodyless POST/DELETE) plus the CSRF header. `body` is serialized when given.
 */
export function sendJson(
  url: string,
  method: string,
  body?: unknown,
): Promise<Response> {
  const init: RequestInit = {
    method,
    headers: { 'content-type': 'application/json' },
  };
  if (body !== undefined) {
    init.body = JSON.stringify(body);
  }
  return csrfFetch(url, init);
}
