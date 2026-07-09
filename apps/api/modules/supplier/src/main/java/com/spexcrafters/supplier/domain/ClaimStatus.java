package com.spexcrafters.supplier.domain;

/**
 * Status of a declared supplier capability (supplier-domain-model §3). A claim NEVER becomes
 * {@code VERIFIED} without an explicit reviewer verification action — there is no auto-verify
 * from a declaration, and no generic {@code verified} boolean anywhere.
 */
public enum ClaimStatus {
    CLAIMED,
    EVIDENCE_SUBMITTED,
    VERIFIED,
    REJECTED
}
