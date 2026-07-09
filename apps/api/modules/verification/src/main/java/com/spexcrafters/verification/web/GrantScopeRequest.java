package com.spexcrafters.verification.web;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Body of {@code grantVerificationScope}. At least one evidence id is required (evidence
 * linkage invariant); {@code validUntil} optionally bounds validity.
 */
public record GrantScopeRequest(
        @NotEmpty List<UUID> evidenceIds,
        Instant validUntil,
        @Size(max = 4000) String reason) {
}
