package com.spexcrafters.identity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body of {@code POST /api/v1/auth/verify-email}. */
public record VerifyEmailRequest(
        @NotBlank @Size(min = 32, max = 512) String token) {
}
