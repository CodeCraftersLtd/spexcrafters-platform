/**
 * Shared cookie constants. Kept dependency-free so both the Edge middleware
 * and Node route handlers can import them without pulling in crypto code.
 */
export const SESSION_COOKIE_NAME = 'sc_session';

/** Session cookie lifetime in seconds (30 days, bounded by the JWE `exp`). */
export const SESSION_COOKIE_MAX_AGE = 60 * 60 * 24 * 30;
