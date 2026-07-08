package com.spexcrafters.organizations.api;

import com.spexcrafters.organizations.domain.InvitationStatus;
import com.spexcrafters.organizations.domain.OrganizationRole;
import java.time.Instant;
import java.util.UUID;

/**
 * The {@code InvitationResponse} schema of the OpenAPI contract. Deliberately excludes any
 * token material — the raw token is delivered solely via the invitation email.
 */
public record InvitationDto(
        UUID id,
        String email,
        OrganizationRole role,
        InvitationStatus status,
        Instant expiresAt,
        Instant createdAt) {
}
