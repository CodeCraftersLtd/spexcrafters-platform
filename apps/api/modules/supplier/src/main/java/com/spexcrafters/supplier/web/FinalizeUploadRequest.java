package com.spexcrafters.supplier.web;

import jakarta.validation.constraints.Size;

/**
 * Body of {@code finalizeEvidenceUpload}. {@code expectedSha256} is optional; when present it
 * is cross-checked against the server-computed digest (mismatch → the upload is rejected).
 */
public record FinalizeUploadRequest(@Size(min = 64, max = 64) String expectedSha256) {
}
