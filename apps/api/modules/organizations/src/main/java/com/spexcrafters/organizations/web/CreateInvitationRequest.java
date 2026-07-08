package com.spexcrafters.organizations.web;

import com.spexcrafters.organizations.domain.OrganizationRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The {@code CreateInvitationRequest} schema of the OpenAPI contract. The contract limits
 * {@code role} to ADMIN|MEMBER; a submitted OWNER value is rejected with a 422 field error
 * by the invitation service (OWNER cannot be invited; promote after joining).
 */
public record CreateInvitationRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull OrganizationRole role) {
}
