package com.spexcrafters.identity.domain;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates opaque tokens (refresh and email verification tokens): 32 bytes of CSPRNG
 * entropy, URL-safe base64 without padding — a 43-character string, satisfying the
 * contract's 32-character minimum and safe to embed in links and JSON.
 */
public final class OpaqueTokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private OpaqueTokenGenerator() {
    }

    public static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
