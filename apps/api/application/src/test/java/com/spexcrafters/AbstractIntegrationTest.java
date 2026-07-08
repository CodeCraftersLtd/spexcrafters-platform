package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spexcrafters.support.RecordingMailConfig;
import com.spexcrafters.support.RecordingMailSender;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for full-stack integration tests: real HTTP against a random port, real PostgreSQL 17
 * via Testcontainers (singleton container shared by all subclasses; Flyway migrates it on
 * first context start), recorded email instead of SMTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RecordingMailConfig.class)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    private static final Pattern TOKEN_IN_LINK = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected RecordingMailSender mailRecorder;

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected ResponseEntity<String> postJson(String path, Map<String, ?> body) {
        return postJson(path, body, new HttpHeaders());
    }

    protected ResponseEntity<String> postJson(String path, Map<String, ?> body, HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            String json = objectMapper.writeValueAsString(body);
            return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(json, headers), String.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalArgumentException("Could not serialise test request body", ex);
        }
    }

    protected ResponseEntity<String> getWithBearer(String path, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    protected JsonNode json(ResponseEntity<String> response) {
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception ex) {
            throw new IllegalStateException("Response body is not valid JSON: " + response.getBody(), ex);
        }
    }

    // ------------------------------------------------------------------ flow helpers

    /** Registers a user and returns the new user id. */
    protected String registerUser(String email, String password) {
        ResponseEntity<String> response = postJson("/api/v1/auth/register", Map.of(
                "email", email,
                "password", password,
                "displayName", "Integration Tester"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return json(response).get("userId").asText();
    }

    /** Waits for the (async) verification email and extracts the raw token from the link. */
    protected String awaitVerificationToken(String email) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> mailRecorder.latestMessageTo(email).isPresent());
        MimeMessage message = mailRecorder.latestMessageTo(email).orElseThrow();
        Matcher matcher = TOKEN_IN_LINK.matcher(mailRecorder.bodyText(message));
        assertThat(matcher.find())
                .as("verification email should contain a token link")
                .isTrue();
        return matcher.group(1);
    }

    /** Registers and verifies a user, ready for login. */
    protected void registerAndVerify(String email, String password) {
        registerUser(email, password);
        ResponseEntity<String> response =
                postJson("/api/v1/auth/verify-email", Map.of("token", awaitVerificationToken(email)));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    /** Logs in and returns the parsed {@code TokenResponse}. */
    protected JsonNode login(String email, String password) {
        ResponseEntity<String> response = postJson("/api/v1/auth/login", Map.of(
                "email", email,
                "password", password));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return json(response);
    }
}
