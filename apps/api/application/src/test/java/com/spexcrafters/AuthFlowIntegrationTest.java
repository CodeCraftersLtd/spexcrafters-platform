package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Walking-skeleton proof: register → verify (token from recorded email) → login → /me. */
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "sturdy-passphrase-42";

    @Test
    void fullRegisterVerifyLoginMeFlow() {
        String email = uniqueEmail();

        // Register: 201 with a UUIDv7 user id.
        String userId = registerUser(email, PASSWORD);
        assertThat(userId).isNotBlank();

        // Login before verification: 403 with the email-not-verified problem type.
        ResponseEntity<String> prematureLogin = postJson("/api/v1/auth/login",
                Map.of("email", email, "password", PASSWORD));
        assertThat(prematureLogin.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(prematureLogin.getHeaders().getContentType())
                .hasToString("application/problem+json");
        assertThat(json(prematureLogin).get("type").asText()).endsWith("/problems/email-not-verified");

        // Verify with the raw token from the recorded email.
        String token = awaitVerificationToken(email);
        ResponseEntity<String> verify = postJson("/api/v1/auth/verify-email", Map.of("token", token));
        assertThat(verify.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Re-submitting the consumed token for a verified user is idempotent (204).
        ResponseEntity<String> verifyAgain = postJson("/api/v1/auth/verify-email", Map.of("token", token));
        assertThat(verifyAgain.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Login: full TokenResponse per contract.
        JsonNode tokens = login(email, PASSWORD);
        assertThat(tokens.get("accessToken").asText()).isNotBlank();
        assertThat(tokens.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(tokens.get("expiresIn").asLong()).isEqualTo(600L);
        assertThat(tokens.get("refreshToken").asText()).hasSizeGreaterThanOrEqualTo(32);
        JsonNode user = tokens.get("user");
        assertThat(user.get("id").asText()).isEqualTo(userId);
        assertThat(user.get("email").asText()).isEqualTo(email);
        assertThat(user.get("displayName").asText()).isEqualTo("Integration Tester");
        assertThat(user.get("locale").asText()).isEqualTo("en");
        assertThat(user.get("emailVerified").asBoolean()).isTrue();
        assertThat(user.get("createdAt").asText()).isNotBlank();

        // /me with the bearer token.
        ResponseEntity<String> me = getWithBearer("/api/v1/me", tokens.get("accessToken").asText());
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(me).get("id").asText()).isEqualTo(userId);
        assertThat(json(me).get("email").asText()).isEqualTo(email);
    }

    @Test
    void meWithoutBearerTokenIsProblem401() {
        ResponseEntity<String> response = rest.getForEntity("/api/v1/me", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // Written by the security entry point, which appends a charset parameter.
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString()).startsWith("application/problem+json");
        JsonNode problem = json(response);
        assertThat(problem.get("type").asText()).endsWith("/problems/authentication-failed");
        assertThat(problem.get("status").asInt()).isEqualTo(401);
        assertThat(problem.get("correlationId").asText()).isNotBlank();
    }

    @Test
    void loginWithWrongPasswordIsProblem401() {
        String email = uniqueEmail();
        registerAndVerify(email, PASSWORD);

        ResponseEntity<String> response = postJson("/api/v1/auth/login",
                Map.of("email", email, "password", "definitely-not-it-9"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(json(response).get("type").asText()).endsWith("/problems/authentication-failed");
    }

    @Test
    void resendVerificationIsAlwaysAccepted() {
        String registered = uniqueEmail();
        registerUser(registered, PASSWORD);

        // Existing unverified account and a completely unknown address behave identically.
        ResponseEntity<String> known = postJson("/api/v1/auth/resend-verification",
                Map.of("email", registered));
        ResponseEntity<String> unknown = postJson("/api/v1/auth/resend-verification",
                Map.of("email", uniqueEmail()));

        assertThat(known.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void verifyWithUnknownTokenIsProblem410() {
        ResponseEntity<String> response = postJson("/api/v1/auth/verify-email",
                Map.of("token", "0".repeat(43)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(json(response).get("type").asText()).endsWith("/problems/token-gone");
    }
}
