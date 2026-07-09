package com.spexcrafters.supplier.api;

import com.spexcrafters.supplier.domain.ApplicationStatus;
import java.time.Instant;
import java.util.UUID;

/** A row of the reviewer queue (the legal name is the reviewer's primary identifier). */
public record ReviewQueueItemDto(
        UUID applicationId,
        UUID supplierId,
        UUID organizationId,
        ApplicationStatus status,
        String legalName,
        String originalLocale,
        Instant submittedAt) {
}
