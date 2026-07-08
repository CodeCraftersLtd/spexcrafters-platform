package com.spexcrafters.identity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body of {@code POST /api/v1/auth/refresh} and {@code POST /api/v1/auth/logout}. */
public record RefreshRequest(
        @NotBlank @Size(min = 32, max = 512) String refreshToken) {
}
