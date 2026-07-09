package com.spexcrafters.supplier.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code initiateEvidenceUpload}. */
public record InitiateUploadRequest(
        @NotBlank @Size(max = 64) String evidenceTypeCode,
        @NotBlank @Size(max = 300) String filename,
        @NotBlank @Size(max = 100) String mediaType,
        @Size(max = 16) String documentLocale) {
}
