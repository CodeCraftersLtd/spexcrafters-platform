package com.spexcrafters.supplier.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code respondToChangeRequest}. */
public record RespondChangeRequestRequest(
        @NotBlank @Size(max = 4000) String response,
        @Size(max = 16) String responseLocale) {
}
