package com.spexcrafters.verification.api;

import com.spexcrafters.verification.domain.VerificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A supplier's verification status: the case rollup plus the authoritative per-scope results.
 * There is no {@code verified} boolean — callers read scope statuses.
 */
public record VerificationStatusDto(
        UUID supplierId,
        VerificationStatus caseStatus,
        Instant openedAt,
        List<VerificationScopeResultDto> scopes) {
}
