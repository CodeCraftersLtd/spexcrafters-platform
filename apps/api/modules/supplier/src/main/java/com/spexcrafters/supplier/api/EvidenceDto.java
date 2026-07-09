package com.spexcrafters.supplier.api;

import com.spexcrafters.supplier.domain.EvidenceReviewStatus;
import com.spexcrafters.supplier.domain.EvidenceUploadState;
import com.spexcrafters.supplier.domain.RetentionStatus;
import com.spexcrafters.supplier.domain.ScanStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Evidence metadata (never bytes, never storage keys, never signed URLs). {@code downloadable}
 * is the fail-closed derived flag the client uses to decide whether to offer a download.
 */
public record EvidenceDto(
        UUID id,
        UUID supplierId,
        String evidenceTypeCode,
        String originalFilename,
        String mediaType,
        Long byteSize,
        String sha256,
        String documentLocale,
        EvidenceUploadState uploadState,
        ScanStatus scanStatus,
        EvidenceReviewStatus reviewStatus,
        RetentionStatus retentionStatus,
        boolean downloadable,
        Instant uploadedAt) {
}
