package com.spexcrafters.identity.web;

import java.util.UUID;

/** Response body of {@code POST /api/v1/auth/register} (201). */
public record RegisterResponse(UUID userId) {
}
