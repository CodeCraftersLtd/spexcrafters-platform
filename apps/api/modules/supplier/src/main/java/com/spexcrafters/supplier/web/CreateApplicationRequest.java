package com.spexcrafters.supplier.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Body of {@code createSupplierApplication}. */
public record CreateApplicationRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 16) String originalLocale,
        @NotBlank @Size(max = 300) String legalName) {
}
