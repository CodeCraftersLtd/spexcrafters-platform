package com.spexcrafters.supplier.api;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * The result of initiating an evidence upload: the server-created evidence id plus the
 * short-lived presigned direct-to-storage PUT the client must use. {@code url} and
 * {@code requiredHeaders} are single-use secrets — the server never logs them.
 */
public record EvidenceUploadTicketDto(
        UUID evidenceId,
        String method,
        URI url,
        Instant expiresAt,
        Map<String, String> requiredHeaders,
        long maxBytes) {
}
