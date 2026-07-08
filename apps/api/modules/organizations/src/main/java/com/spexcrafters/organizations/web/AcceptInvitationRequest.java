package com.spexcrafters.organizations.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The {@code AcceptInvitationRequest} schema of the OpenAPI contract. */
public record AcceptInvitationRequest(@NotBlank @Size(min = 32, max = 512) String token) {
}
