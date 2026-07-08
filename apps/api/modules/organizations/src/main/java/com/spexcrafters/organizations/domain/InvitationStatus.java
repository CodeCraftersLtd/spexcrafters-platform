package com.spexcrafters.organizations.domain;

/** Invitation lifecycle; {@code PENDING} is the only state from which transitions occur. */
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    REVOKED,
    EXPIRED
}
