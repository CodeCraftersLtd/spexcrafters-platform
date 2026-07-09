package com.spexcrafters.supplier.api;

import com.spexcrafters.supplier.domain.ApplicationStatus;
import com.spexcrafters.supplier.domain.OperationalStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The {@code SupplierApplicationResponse} schema: the application plus its supplier's
 * operational status and the caller's resolved supplier capabilities. {@code version} feeds
 * the optimistic-locking round trip of draft updates.
 */
public record SupplierApplicationDto(
        UUID applicationId,
        UUID supplierId,
        UUID organizationId,
        String originalLocale,
        ApplicationStatus status,
        OperationalStatus operationalStatus,
        Instant submittedAt,
        Instant decidedAt,
        Instant createdAt,
        int version,
        List<SupplierCapability> callerCapabilities) {
}
