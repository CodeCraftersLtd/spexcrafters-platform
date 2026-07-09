package com.spexcrafters.verification.domain;

/**
 * Verification status for a case and its scope results (supplier-domain-model §3). Verification
 * is scope-based: there is <strong>no generic {@code verified} boolean</strong> anywhere. A
 * scope reaches {@code VERIFIED} only by an explicit platform-staff grant backed by evidence.
 */
public enum VerificationStatus {
    NOT_REQUESTED,
    PENDING,
    UNDER_REVIEW,
    VERIFIED,
    REJECTED,
    CHANGES_REQUESTED,
    EXPIRED,
    SUSPENDED,
    REVOKED
}
