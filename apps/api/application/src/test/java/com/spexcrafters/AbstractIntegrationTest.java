package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spexcrafters.support.RecordingMailConfig;
import com.spexcrafters.support.RecordingMailSender;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /**
     * TestRestTemplate's httpclient5 client, by default, honors a 429/503 {@code Retry-After}
     * header and transparently re-executes the request after sleeping — which turned the
     * login-throttle test into a ~15-minute wall-clock hang (Retry-After ≈ 900s) and would
     * silently convert an asserted 429 into a later 401. Disable client-side auto-retry so
     * throttled responses are observed directly and immediately. Production behavior is
     * unaffected (this only configures the test HTTP client).
     */
    @BeforeEach
    void disableTestClientAutoRetry() {
        rest.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory(
                HttpClients.custom().disableAutomaticRetries().build()));
    }

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

    // ------------------------------------------------------------------ audit helpers

    /** Actions recorded in {@code audit.audit_log} by {@code actorUserId}, oldest first. */
    protected List<String> auditActionsBy(String actorUserId) {
        return jdbcTemplate.queryForList(
                "select action from audit.audit_log where actor_user_id = ? order by at, id",
                String.class, UUID.fromString(actorUserId));
    }

    protected long countAuditRows(String action, String actorUserId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from audit.audit_log where action = ? and actor_user_id = ?",
                Long.class, action, UUID.fromString(actorUserId));
        return count == null ? 0 : count;
    }

    /**
     * The jsonb {@code detail} payloads of matching audit rows, normalized to compact JSON.
     * PostgreSQL renders jsonb text with a space after every colon/comma; re-serializing
     * through Jackson yields the compact form the assertions match against, so tests do not
     * depend on the driver's whitespace.
     */
    protected List<String> auditDetails(String action, String actorUserId) {
        return jdbcTemplate
                .queryForList(
                        "select detail::text from audit.audit_log where action = ? and actor_user_id = ?"
                                + " order by at, id",
                        String.class, action, UUID.fromString(actorUserId))
                .stream()
                .map(this::compactJson)
                .toList();
    }

    private String compactJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(objectMapper.readTree(json));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return json;
        }
    }

    /** The {@code target_id} values of matching audit rows, oldest first. */
    protected List<String> auditTargetIds(String action, String actorUserId) {
        return jdbcTemplate.queryForList(
                "select target_id from audit.audit_log where action = ? and actor_user_id = ?"
                        + " order by at, id",
                String.class, action, UUID.fromString(actorUserId));
    }
}
