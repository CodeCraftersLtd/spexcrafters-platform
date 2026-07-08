/**
 * Shared cookie constants. Kept dependency-free so both the Edge middleware
 * and Node route handlers can import them without pulling in crypto code.
 */
export const SESSION_COOKIE_NAME = 'sc_session';

/**
 * CSRF token transport cookie (ADR-018). Carries the same value sealed inside
 * the JWE session, but is deliberately **not** HttpOnly so same-origin client
 * code can read it and echo it in the `X-CSRF-Token` header. It is not a
 * credential: replaying it without the HttpOnly `sc_session` cookie authorizes
 * nothing.
 */
export const CSRF_COOKIE_NAME = 'sc_csrf';

/** Session cookie lifetime in seconds (30 days, bounded by the JWE `exp`). */
export const SESSION_COOKIE_MAX_AGE = 60 * 60 * 24 * 30;
