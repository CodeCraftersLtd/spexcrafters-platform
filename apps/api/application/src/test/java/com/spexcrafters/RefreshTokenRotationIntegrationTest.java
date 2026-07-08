package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/**
 * Single-use rotation, the Phase-6 concurrency grace window and family-revocation-on-replay
 * semantics of refresh tokens (session-security-policy.md §2).
 *
 * <p>The grace window is shortened to 2 s so post-grace replay is testable with a small
 * sleep; reuse "within grace" happens immediately after rotation, far inside 2 s.
 */
@TestPropertySource(properties = "spexcrafters.auth.refresh-grace=2s")
class RefreshTokenRotationIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "sturdy-passphrase-42";
    /** Keep in sync with the {@code @TestPropertySource} above. */
    private static final Duration GRACE = Duration.ofSeconds(2);

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
    void reuseWithinTheGraceWindowIs401WithoutRevokingTheFamily() {
        String email = uniqueEmail();
        registerAndVerify(email, PASSWORD);
        JsonNode session = login(email, PASSWORD);
        String userId = session.get("user").get("id").asText();
        String firstToken = session.get("refreshToken").asText();

        // Legitimate rotation: firstToken -> secondToken.
        ResponseEntity<String> rotation = postJson("/api/v1/auth/refresh",
                Map.of("refreshToken", firstToken));
        assertThat(rotation.getStatusCode()).isEqualTo(HttpStatus.OK);
        String secondToken = json(rotation).get("refreshToken").asText();

        // Immediate reuse of the rotated token (a benign multi-tab race): 401, but the
        // family stays alive — no theft response.
        ResponseEntity<String> reuse = postJson("/api/v1/auth/refresh",
                Map.of("refreshToken", firstToken));
        assertThat(reuse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // Distinct problem type so the BFF keeps the session alive on a benign race
        // instead of signing the user out (see ADR-018 / session.ts refreshIfNeeded).
        assertThat(json(reuse).get("type").asText()).endsWith("/problems/concurrent-refresh");
        assertThat(json(reuse).get("detail").asText()).containsIgnoringCase("concurrent");

        // The successor keeps working: the family was NOT revoked.
        ResponseEntity<String> successorRefresh = postJson("/api/v1/auth/refresh",
                Map.of("refreshToken", secondToken));
        assertThat(successorRefresh.getStatusCode()).isEqualTo(HttpStatus.OK);

        // No replay/theft audit events for a benign race.
        assertThat(countAuditRows("identity.session.replay_detected", userId)).isZero();
        assertThat(countAuditRows("identity.session.family_revoked", userId)).isZero();
    }

    @Test
    void reuseAfterTheGraceWindowRevokesTheWholeFamilyAndAudits() throws InterruptedException {
        String email = uniqueEmail();
        registerAndVerify(email, PASSWORD);
        JsonNode session = login(email, PASSWORD);
        String userId = session.get("user").get("id").asText();
        String firstToken = session.get("refreshToken").asText();

        // Legitimate rotation: firstToken -> secondToken.
        ResponseEntity<String> rotation = postJson("/api/v1/auth/refresh",
                Map.of("refreshToken", firstToken));
        assertThat(rotation.getStatusCode()).isEqualTo(HttpStatus.OK);
        String secondToken = json(rotation).get("refreshToken").asText();

        // Outlast the grace window, then replay the consumed token: theft.
        Thread.sleep(GRACE.plusMillis(500).toMillis());
        ResponseEntity<String> replay = postJson("/api/v1/auth/refresh",
                Map.of("refreshToken", firstToken));
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(json(replay).get("type").asText()).endsWith("/problems/authentication-failed");

        // Both audit events exist, carrying the family id in the jsonb detail.
        String familyId = familyIdOf(userId);
        assertThat(auditDetails("identity.session.replay_detected", userId))
                .singleElement().asString().contains("\"familyId\"").contains(familyId);
        assertThat(auditDetails("identity.session.family_revoked", userId))
                .singleElement().asString().contains("\"familyId\"").contains(familyId);

        // The revocation cascaded to the whole family: the still-fresh second token is dead too.
        ResponseEntity<String> afterRevocation = postJson("/api/v1/auth/refresh",
                Map.of("refreshToken", secondToken));
        assertThat(afterRevocation.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Presenting further dead tokens does not spam family_revoked (nothing left to revoke).
        assertThat(countAuditRows("identity.session.family_revoked", userId)).isEqualTo(1);
    }

    @Test
    void parallelRefreshesOfTheSameTokenRotateExactlyOnce() throws Exception {
        String email = uniqueEmail();
        registerAndVerify(email, PASSWORD);
        JsonNode session = login(email, PASSWORD);
        String userId = session.get("user").get("id").asText();
        String refreshToken = session.get("refreshToken").asText();

        int threads = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Callable<ResponseEntity<String>> attempt = () -> {
                start.await();
                return postJson("/api/v1/auth/refresh", Map.of("refreshToken", refreshToken));
            };
            List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(attempt));
            }
            start.countDown();

            List<ResponseEntity<String>> responses = new ArrayList<>();
            for (Future<ResponseEntity<String>> future : futures) {
                responses.add(future.get());
            }

            // Exactly one rotation wins; every loser is 401 (benign concurrent refresh).
            List<ResponseEntity<String>> winners = responses.stream()
                    .filter(response -> response.getStatusCode() == HttpStatus.OK)
                    .toList();
            assertThat(winners).hasSize(1);
            assertThat(responses).filteredOn(r -> r.getStatusCode() == HttpStatus.UNAUTHORIZED)
                    .hasSize(threads - 1)
                    .allSatisfy(response -> assertThat(json(response).get("type").asText())
                            .endsWith("/problems/authentication-failed"));

            // The family survived the race: the winner's successor token still refreshes.
            String successor = json(winners.get(0)).get("refreshToken").asText();
            ResponseEntity<String> next = postJson("/api/v1/auth/refresh",
                    Map.of("refreshToken", successor));
            assertThat(next.getStatusCode()).isEqualTo(HttpStatus.OK);

            // And none of the losers was treated as theft.
            assertThat(countAuditRows("identity.session.replay_detected", userId)).isZero();
            assertThat(countAuditRows("identity.session.family_revoked", userId)).isZero();
        } finally {
            pool.shutdownNow();
        }
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

    /** The single refresh-token family id of {@code userId} (one login per test user). */
    private String familyIdOf(String userId) {
        return jdbcTemplate.queryForObject(
                "select distinct family_id::text from identity.refresh_token where user_id = ?",
                String.class, UUID.fromString(userId));
    }
}
