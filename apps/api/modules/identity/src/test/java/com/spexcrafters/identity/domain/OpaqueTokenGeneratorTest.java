package com.spexcrafters.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OpaqueTokenGeneratorTest {

    @Test
    void generatesUrlSafeTokensMeetingTheContractMinimumLength() {
        String token = OpaqueTokenGenerator.generate();

        // Contract requires 32..512 characters; 32 random bytes base64url = 43 characters.
        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void generatesUniqueTokens() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            tokens.add(OpaqueTokenGenerator.generate());
        }

        assertThat(tokens).hasSize(1_000);
    }
}
