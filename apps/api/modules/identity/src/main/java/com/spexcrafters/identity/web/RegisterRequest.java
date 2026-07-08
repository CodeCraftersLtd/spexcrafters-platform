package com.spexcrafters.identity.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body of {@code POST /api/v1/auth/register}. {@code locale} defaults to {@code en}. */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 128) String password,
        @NotBlank @Size(max = 120) String displayName,
        UserLocale locale) {
}
