package com.spexcrafters.identity.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hashes opaque tokens (verification and refresh tokens) with SHA-256 before storage,
 * so a database leak does not expose usable tokens. The tokens carry 256 bits of
 * CSPRNG entropy, so a fast unsalted hash is appropriate here (unlike passwords).
 */
public final class TokenHasher {

    private TokenHasher() {
    }

    /** Returns the lowercase hex SHA-256 digest of the UTF-8 bytes of {@code rawToken}. */
    public static String sha256Hex(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated for every JVM; this cannot happen.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
