package com.spexcrafters.organizations.domain;

/**
 * Membership lifecycle. {@code REMOVED} is terminal — removed rows are retained for audit
 * and a re-join creates a new membership row (partial unique index on
 * {@code (organization_id, user_id) WHERE status = 'ACTIVE'}).
 */
public enum MembershipStatus {
    ACTIVE,
    REMOVED
}
