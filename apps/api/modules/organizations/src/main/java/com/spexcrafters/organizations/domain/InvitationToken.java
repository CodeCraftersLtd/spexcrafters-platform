package com.spexcrafters.organizations.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Invitation token generation and hashing: 32 bytes of CSPRNG entropy encoded as URL-safe
 * base64 without padding (43 characters, link- and JSON-safe), stored only as a SHA-256
 * hash so a database leak never exposes usable invitations. The tokens carry 256 bits of
 * entropy, so a fast unsalted hash is appropriate (unlike passwords).
 */
public final class InvitationToken {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private InvitationToken() {
    }

    /** Generates a new raw invitation token. Never persist or log the returned value. */
    public static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
