package com.spexcrafters.identity.domain;

/** Lifecycle state of a {@link UserAccount}. Stored as a string column. */
public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    SUSPENDED
}
