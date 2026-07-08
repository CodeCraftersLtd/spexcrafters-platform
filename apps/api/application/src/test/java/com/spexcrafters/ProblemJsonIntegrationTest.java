package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** RFC 9457 problem+json shape: type/title/status/correlationId/errors[], per the contract. */
class ProblemJsonIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "sturdy-passphrase-42";

    @Test
    void beanValidationFailureIs422WithFieldErrors() {
        ResponseEntity<String> response = postJson("/api/v1/auth/register", Map.of(
                "email", "not-an-email",
                "password", "short",
                "displayName", ""));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getHeaders().getContentType()).hasToString("application/problem+json");

        JsonNode problem = json(response);
        assertThat(problem.get("type").asText()).endsWith("/problems/validation-error");
        assertThat(problem.get("title").asText()).isEqualTo("Validation failed");
        assertThat(problem.get("status").asInt()).isEqualTo(422);
        assertThat(problem.get("correlationId").asText()).isNotBlank();

        List<String> fields = new ArrayList<>();
        problem.get("errors").forEach(error -> {
            fields.add(error.get("field").asText());
            assertThat(error.get("code").asText()).isNotBlank();
            assertThat(error.get("message").asText()).isNotBlank();
        });
        assertThat(fields).contains("email", "password", "displayName");
    }

    @Test
    void serverSidePasswordPolicyViolationIs422WithStableCodes() {
        // Passes the declarative min-length check but has no digit.
        ResponseEntity<String> response = postJson("/api/v1/auth/register", Map.of(
                "email", uniqueEmail(),
                "password", "onlylettersinside",
                "displayName", "Policy Tester"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        JsonNode error = json(response).get("errors").get(0);
        assertThat(error.get("field").asText()).isEqualTo("password");
        assertThat(error.get("code").asText()).isEqualTo("password.needs_digit");
    }

    @Test
    void duplicateRegistrationIs409ConflictProblem() {
        String email = uniqueEmail();
        registerUser(email, PASSWORD);

        ResponseEntity<String> response = postJson("/api/v1/auth/register", Map.of(
                "email", email,
                "password", PASSWORD,
                "displayName", "Second Account"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getContentType()).hasToString("application/problem+json");
        JsonNode problem = json(response);
        assertThat(problem.get("type").asText()).endsWith("/problems/conflict");
        assertThat(problem.get("status").asInt()).isEqualTo(409);
    }

    @Test
    void providedCorrelationIdIsEchoedInHeaderAndProblemBody() {
        String correlationId = "it-corr-0123456789";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Correlation-Id", correlationId);

        ResponseEntity<String> response = postJson("/api/v1/auth/login",
                Map.of("email", uniqueEmail(), "password", "wrong-password-1"), headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isEqualTo(correlationId);
        assertThat(json(response).get("correlationId").asText()).isEqualTo(correlationId);
    }

    @Test
    void repeatedLoginFailuresAreThrottledWith429AndRetryAfter() {
        String email = uniqueEmail(); // never registered; failures still count per email

        for (int i = 0; i < 6; i++) {
            ResponseEntity<String> failure = postJson("/api/v1/auth/login",
                    Map.of("email", email, "password", "wrong-password-1"));
            assertThat(failure.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<String> throttled = postJson("/api/v1/auth/login",
                Map.of("email", email, "password", "wrong-password-1"));

        assertThat(throttled.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(throttled.getHeaders().getContentType()).hasToString("application/problem+json");
        JsonNode problem = json(throttled);
        assertThat(problem.get("type").asText()).endsWith("/problems/rate-limited");
        String retryAfter = throttled.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        assertThat(retryAfter).isNotNull();
        assertThat(Long.parseLong(retryAfter)).isBetween(1L, 900L);
    }
}
