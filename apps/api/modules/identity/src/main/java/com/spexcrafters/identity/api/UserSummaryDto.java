package com.spexcrafters.identity.api;

import java.time.Instant;
import java.util.UUID;

/**
 * The {@code UserSummary} schema of the OpenAPI contract. {@code locale} is the wire code
 * (e.g. {@code en}, {@code zh-Hans}) rather than an enum so future locales remain
 * backward-compatible for clients.
 */
public record UserSummaryDto(
        UUID id,
        String email,
        String displayName,
        String locale,
        boolean emailVerified,
        Instant createdAt) {
}
