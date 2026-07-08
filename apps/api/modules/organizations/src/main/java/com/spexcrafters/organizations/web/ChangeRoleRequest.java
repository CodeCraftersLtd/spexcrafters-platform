package com.spexcrafters.organizations.web;

import com.spexcrafters.organizations.domain.OrganizationRole;
import jakarta.validation.constraints.NotNull;

/** The {@code ChangeRoleRequest} schema of the OpenAPI contract. */
public record ChangeRoleRequest(@NotNull OrganizationRole role) {
}
