package com.spexcrafters.identity.domain;

/**
 * Result of a login attempt. Only {@link #FAILURE} (bad credentials / unknown account)
 * counts towards the brute-force window; state-based rejections are recorded under their
 * own outcome so a legitimate user cannot be locked out by e.g. clicking login before
 * verifying their email.
 */
public enum LoginOutcome {
    SUCCESS,
    FAILURE,
    THROTTLED,
    EMAIL_NOT_VERIFIED,
    SUSPENDED
}
