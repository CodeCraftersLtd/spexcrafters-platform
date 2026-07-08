package com.spexcrafters.organizations.api;

import com.spexcrafters.organizations.domain.Capability;
import com.spexcrafters.organizations.domain.OrganizationRole;
import com.spexcrafters.organizations.domain.OrganizationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The {@code OrganizationResponse} schema of the OpenAPI contract: the organization plus
 * the caller's own role and resolved capabilities (wire names). {@code version} feeds the
 * optimistic-locking round trip of {@code updateOrganization}.
 */
public record OrganizationDto(
        UUID id,
        String name,
        OrganizationType type,
        String country,
        Instant createdAt,
        int version,
        OrganizationRole callerRole,
        List<Capability> callerCapabilities) {
}
