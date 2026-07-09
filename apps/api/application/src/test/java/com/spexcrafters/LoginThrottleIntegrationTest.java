package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.spexcrafters.support.MutableClock;
import com.spexcrafters.support.MutableClockConfig;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Proves the full login-throttle lifecycle deterministically, with no real wall-clock waiting:
 * activation after too many failures, the 429 problem+json + {@code Retry-After} contract, that
 * even a correct password stays throttled while locked, and that the window RESETS once time
 * advances past it so a correct login then succeeds.
 *
 * <p>Time is advanced via an injected {@link MutableClock} (this class only) — the production
 * throttle window ({@code LoginThrottle.WINDOW} = 15 minutes) is unchanged. The base class
 * disables the test HTTP client's Retry-After auto-retry, so the 429 is observed directly.
 */
@Import(MutableClockConfig.class)
class LoginThrottleIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "sturdy-passphrase-42";

    @Autowired
    private MutableClock clock;

    @Test
    void throttleActivatesReturns429ThenResetsAfterTheWindow() {
        String email = uniqueEmail();
        registerAndVerify(email, PASSWORD);

        // Activation: 6 credential failures (MAX_FAILURES = 5) are each rejected 401.
        for (int i = 0; i < 6; i++) {
            ResponseEntity<String> failure = postJson("/api/v1/auth/login",
                    Map.of("email", email, "password", "wrong-password"));
            assertThat(failure.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        // The next attempt is throttled: 429 + problem+json + Retry-After (near the 15m window).
        ResponseEntity<String> throttled = postJson("/api/v1/auth/login",
                Map.of("email", email, "password", "wrong-password"));
        assertThat(throttled.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(throttled.getHeaders().getContentType()).hasToString("application/problem+json");
        JsonNode problem = json(throttled);
        assertThat(problem.get("type").asText()).endsWith("/problems/rate-limited");
        String retryAfter = throttled.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        assertThat(retryAfter).isNotNull();
        long retryAfterSeconds = Long.parseLong(retryAfter);
        assertThat(retryAfterSeconds).isBetween(1L, 900L);

        // While locked, even the CORRECT password is throttled (the check precedes auth).
        ResponseEntity<String> stillLocked = postJson("/api/v1/auth/login",
                Map.of("email", email, "password", PASSWORD));
        assertThat(stillLocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Reset: advance past the window so the failures age out — no real waiting.
        clock.advance(Duration.ofSeconds(retryAfterSeconds).plusMinutes(1));

        // Success after reset: the correct password now logs in.
        ResponseEntity<String> success = postJson("/api/v1/auth/login",
                Map.of("email", email, "password", PASSWORD));
        assertThat(success.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(success).get("accessToken").asText()).isNotBlank();
    }
}
