package com.spexcrafters.supplier.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code requestSupplierChanges}. */
public record RequestChangesRequest(
        @NotBlank @Size(max = 200) String requestedItem,
        @NotBlank @Size(max = 4000) String reason) {
}
