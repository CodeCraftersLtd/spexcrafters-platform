package com.spexcrafters.identity.api;

/**
 * A freshly issued token pair: signed JWT access token plus the raw (unhashed) opaque
 * refresh token. The raw refresh token exists only in this response; the database holds
 * its SHA-256 hash.
 */
public record IssuedTokens(
        String accessToken,
        long expiresInSeconds,
        String refreshToken,
        UserSummaryDto user) {
}
