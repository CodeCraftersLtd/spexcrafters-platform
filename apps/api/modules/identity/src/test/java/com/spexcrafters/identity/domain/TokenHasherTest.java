package com.spexcrafters.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenHasherTest {

    @Test
    void producesTheKnownSha256TestVector() {
        // NIST test vector: SHA-256("abc")
        assertThat(TokenHasher.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void isDeterministic() {
        String token = "some-opaque-token-value";

        assertThat(TokenHasher.sha256Hex(token)).isEqualTo(TokenHasher.sha256Hex(token));
    }

    @Test
    void producesSixtyFourLowercaseHexCharacters() {
        String hash = TokenHasher.sha256Hex(OpaqueTokenGenerator.generate());

        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
    }
}
