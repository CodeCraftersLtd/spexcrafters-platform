package com.spexcrafters.organizations.api;

import com.spexcrafters.organizations.domain.OrganizationRole;
import java.time.Instant;
import java.util.UUID;

/** The {@code MemberResponse} schema of the OpenAPI contract. */
public record MemberDto(
        UUID membershipId,
        UUID userId,
        String displayName,
        String email,
        OrganizationRole role,
        Instant joinedAt) {
}
