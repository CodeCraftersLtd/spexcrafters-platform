package com.spexcrafters.organizations.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The {@code UpdateOrganizationRequest} schema of the OpenAPI contract. Absent ({@code
 * null}) fields are left unchanged; {@code version} is the optimistic-locking version from
 * the last read — a mismatch is a 409.
 */
public record UpdateOrganizationRequest(
        @Size(min = 2, max = 120) String name,
        @Pattern(regexp = "[A-Za-z]{2}", message = "must be an ISO 3166-1 alpha-2 code") String country,
        @NotNull Integer version) {
}
