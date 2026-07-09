package com.spexcrafters.media.api;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/**
 * A short-lived presigned direct-to-storage upload instruction. {@code url} and {@code
 * requiredHeaders} are sensitive and must never be logged or persisted.
 *
 * @param method          the HTTP method the client must use (always {@code PUT})
 * @param url             the presigned URL the client PUTs the bytes to
 * @param expiresAt       the instant the URL stops being valid
 * @param requiredHeaders headers the client must echo for the signature to match
 *                        (e.g. {@code Content-Type})
 * @param maxBytes        the upper size bound the signature enforces
 */
public record PresignedUpload(
        String method,
        URI url,
        Instant expiresAt,
        Map<String, String> requiredHeaders,
        long maxBytes) {

    public PresignedUpload {
        requiredHeaders = Map.copyOf(requiredHeaders);
    }
}
