package com.spexcrafters.supplier.api;

import com.spexcrafters.supplier.domain.ReviewRequestStatus;
import java.time.Instant;
import java.util.UUID;

/** A change request against an application and the supplier's response, if any. */
public record ReviewRequestDto(
        UUID id,
        UUID applicationId,
        String requestedItem,
        String reason,
        ReviewRequestStatus status,
        String supplierResponse,
        String responseLocale,
        Instant requestedAt,
        Instant resolvedAt) {
}
