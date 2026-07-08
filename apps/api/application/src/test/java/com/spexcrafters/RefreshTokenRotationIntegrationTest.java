package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Single-use rotation and family-revocation-on-reuse semantics of refresh tokens. */
class RefreshTokenRotationIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "sturdy-passphrase-42";

    @Test
    void refreshRotatesToANewTokenPair() {
        String email = uniqueEmail();
        registerAndVerify(email, PASSWORD);
        String originalRefreshToken = login(email, PASSWORD).get("refreshToken").asText();

        ResponseEntity<String> response = postJson("/api/v1/auth/refresh",
                Map.of("refreshToken", originalRefreshToken));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode rotated = json(response);
        assertThat(rotated.get("accessToken").asText()).isNotBlank();
        assertThat(rotated.get("refreshToken").asText())
                .isNotBlank()
                .isNotEqualTo(originalRefreshToken);
        assertThat(rotated.get("user").get("email").asText()).isEqualTo(email);
    }

    @Test
    void reusingAConsumedTokenRevokesTheWholeFamily() {
        String email = uniqueEmail();
        registerAndVerify(email, PASSWORD);
        String firstToken = login(email, PASSWORD).get("refreshToken").asText();

        // Legitimate rotation: firstToken -> secondToken.
        ResponseEntity<String> rotation = postJson("/api/v1/auth/refresh",
                Map.of("refreshToken", firstToken));
        assertThat(rotation.getStatusCode()).isEqualTo(HttpStatus.OK);
        String secondToken = json(rotation).get("refreshToken").asText();

        // Replay of the consumed first token: 401 (theft detected).
        ResponseEntity<String> replay = postJson("/api/v1/auth/refresh",
                Map.of("refreshToken", firstToken));
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(json(replay).get("type").asText()).endsWith("/problems/authentication-failed");

        // The revocation cascaded to the whole family: the still-fresh second token is dead too.
        ResponseEntity<String> afterRevocation = postJson("/api/v1/auth/refresh",
                Map.of("refreshToken", secondToken));
        assertThat(afterRevocation.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutRevokesTheFamilyAndIsIdempotent() {
        String email = uniqueEmail();
        registerAndVerify(email, PASSWORD);
        String refreshToken = login(email, PASSWORD).get("refreshToken").asText();

        ResponseEntity<String> logout = postJson("/api/v1/auth/logout",
                Map.of("refreshToken", refreshToken));
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // The revoked token can no longer be exchanged.
        ResponseEntity<String> refreshAfterLogout = postJson("/api/v1/auth/refresh",
                Map.of("refreshToken", refreshToken));
        assertThat(refreshAfterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Logging out again with the same (now revoked) token is still 204.
        ResponseEntity<String> logoutAgain = postJson("/api/v1/auth/logout",
                Map.of("refreshToken", refreshToken));
        assertThat(logoutAgain.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void refreshWithAnUnknownTokenIsProblem401() {
        ResponseEntity<String> response = postJson("/api/v1/auth/refresh",
                Map.of("refreshToken", "f".repeat(43)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getContentType()).hasToString("application/problem+json");
    }
}
