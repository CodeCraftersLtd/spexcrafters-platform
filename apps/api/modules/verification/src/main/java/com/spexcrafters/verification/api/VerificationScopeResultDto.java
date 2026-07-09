package com.spexcrafters.verification.api;

import com.spexcrafters.verification.domain.VerificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A per-scope verification result with its evidence linkage (evidence ids only). */
public record VerificationScopeResultDto(
        String scopeCode,
        VerificationStatus status,
        Instant decidedAt,
        Instant validFrom,
        Instant validUntil,
        String reason,
        List<UUID> evidenceIds) {
}
