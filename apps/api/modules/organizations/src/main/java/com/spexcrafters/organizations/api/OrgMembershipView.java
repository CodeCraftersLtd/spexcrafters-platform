package com.spexcrafters.organizations.api;

import java.util.UUID;

/**
 * A read-only projection of a user's ACTIVE membership in an organization, for cross-module
 * authorization (supplier capabilities are org-role-derived). Carries no JPA entities.
 *
 * @param organizationId     the organization
 * @param userId             the member
 * @param role               the member's role
 * @param organizationActive whether the organization itself is ACTIVE (not suspended)
 */
public record OrgMembershipView(
        UUID organizationId,
        UUID userId,
        OrgRole role,
        boolean organizationActive) {
}
