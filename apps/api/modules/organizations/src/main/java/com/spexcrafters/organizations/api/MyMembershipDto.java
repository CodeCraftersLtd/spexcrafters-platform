package com.spexcrafters.organizations.api;

import com.spexcrafters.organizations.domain.OrganizationRole;
import com.spexcrafters.organizations.domain.OrganizationType;
import java.time.Instant;
import java.util.UUID;

/** The {@code MyMembership} schema of the OpenAPI contract. */
public record MyMembershipDto(
        UUID membershipId,
        UUID organizationId,
        String organizationName,
        OrganizationType organizationType,
        OrganizationRole role,
        Instant joinedAt) {
}
