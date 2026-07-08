package com.spexcrafters.organizations.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InvitationTokenTest {

    @Test
    void generatesUrlSafe43CharacterTokens() {
        String token = InvitationToken.generate();

        // 32 bytes → 43 base64url characters without padding; safe in links and JSON.
        assertThat(token).hasSize(43);
        assertThat(token).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void tokensAreUnique() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            assertThat(seen.add(InvitationToken.generate())).isTrue();
        }
    }

    @Test
    void hashesWithSha256Hex() {
        // Known SHA-256 vector.
        assertThat(InvitationToken.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        // Deterministic and 64 lowercase hex characters.
        String token = InvitationToken.generate();
        assertThat(InvitationToken.sha256Hex(token))
                .isEqualTo(InvitationToken.sha256Hex(token))
                .hasSize(64)
                .matches("[0-9a-f]+");
    }
}
