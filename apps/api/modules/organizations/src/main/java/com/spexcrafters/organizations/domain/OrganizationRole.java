package com.spexcrafters.organizations.domain;

/**
 * Membership role with a strict rank order ({@code OWNER > ADMIN > MEMBER}) used by the
 * privilege-escalation rank rules of organizations-capability-model.md §2.
 */
public enum OrganizationRole {
    OWNER(3),
    ADMIN(2),
    MEMBER(1);

    private final int rank;

    OrganizationRole(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    /** True when this role's rank is greater than or equal to {@code other}'s. */
    public boolean isAtLeast(OrganizationRole other) {
        return rank >= other.rank;
    }

    /** True when this role's rank is strictly greater than {@code other}'s. */
    public boolean higherThan(OrganizationRole other) {
        return rank > other.rank;
    }
}
