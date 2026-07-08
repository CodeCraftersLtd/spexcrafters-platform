package com.spexcrafters.organizations.web;

import com.spexcrafters.organizations.domain.OrganizationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The {@code CreateOrganizationRequest} schema of the OpenAPI contract. */
public record CreateOrganizationRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotNull OrganizationType type,
        @Pattern(regexp = "[A-Za-z]{2}", message = "must be an ISO 3166-1 alpha-2 code") String country) {
}
