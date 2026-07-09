package com.spexcrafters.supplier.api;

import com.spexcrafters.supplier.domain.ApplicationStatus;
import com.spexcrafters.supplier.domain.OperationalStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The reviewer's full view of an application: lifecycle, profile, evidence and change requests. */
public record ReviewDetailDto(
        UUID applicationId,
        UUID supplierId,
        UUID organizationId,
        ApplicationStatus status,
        OperationalStatus operationalStatus,
        Instant submittedAt,
        int version,
        SupplierProfileDto profile,
        List<EvidenceDto> evidence,
        List<ReviewRequestDto> changeRequests) {
}
