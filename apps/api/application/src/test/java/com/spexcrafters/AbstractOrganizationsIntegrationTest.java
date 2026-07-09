package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.awaitility.Awaitility;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Shared plumbing for the organizations-slice integration tests: authenticated users,
 * bearer-token HTTP helpers for every verb of the contract, invitation-token capture from
 * recorded email (never from the database — only the hash is stored).
 */
public abstract class AbstractOrganizationsIntegrationTest extends AbstractIntegrationTest {

    protected static final String PASSWORD = "sturdy-passphrase-42";

    private static final Pattern TOKEN_IN_LINK = Pattern.compile("token=([A-Za-z0-9_-]+)");

    protected record TestUser(String email, String userId, String accessToken) {
    }

    /** Registers, verifies and logs a fresh user in. */
    protected TestUser signUpUser() {
        String email = uniqueEmail();
        registerAndVerify(email, PASSWORD);
        JsonNode tokens = login(email, PASSWORD);
        return new TestUser(email, tokens.get("user").get("id").asText(),
                tokens.get("accessToken").asText());
    }

    /** Creates an organization as {@code owner} and returns its id. */
    protected String createOrganization(TestUser owner, String name) {
        ResponseEntity<String> response = postJsonWithBearer("/api/v1/organizations",
                Map.of("name", name, "type", "SUPPLIER"), owner.accessToken());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return json(response).get("id").asText();
    }

    /** Invites {@code email} and returns the raw token captured from the recorded email. */
    protected String inviteAndCaptureToken(TestUser actor, String organizationId, String email, String role) {
        ResponseEntity<String> response = postJsonWithBearer(
                "/api/v1/organizations/" + organizationId + "/invitations",
                Map.of("email", email, "role", role), actor.accessToken());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return awaitInvitationToken(email);
    }

    /** Waits for the (async) invitation email and extracts the raw token from the link. */
    protected String awaitInvitationToken(String email) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> mailRecorder.latestMessageTo(email)
                        .map(message -> mailRecorder.bodyText(message).contains("/en/invitations/accept?token="))
                        .orElse(false));
        MimeMessage message = mailRecorder.latestMessageTo(email).orElseThrow();
        Matcher matcher = TOKEN_IN_LINK.matcher(mailRecorder.bodyText(message));
        assertThat(matcher.find())
                .as("invitation email should contain a token link")
                .isTrue();
        return matcher.group(1);
    }

    /** The {@code membershipId} of {@code userId} as seen by {@code viewer} via listMembers. */
    protected String membershipIdOf(TestUser viewer, String organizationId, String userId) {
        ResponseEntity<String> response = getWithBearer(
                "/api/v1/organizations/" + organizationId + "/members", viewer.accessToken());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        for (JsonNode member : json(response)) {
            if (member.get("userId").asText().equals(userId)) {
                return member.get("membershipId").asText();
            }
        }
        throw new IllegalStateException("No member with userId " + userId + " in organization " + organizationId);
    }

    // ------------------------------------------------------------------ HTTP helpers

    protected ResponseEntity<String> postJsonWithBearer(String path, Map<String, ?> body, String accessToken) {
        return exchangeJson(HttpMethod.POST, path, body, accessToken);
    }

    protected ResponseEntity<String> patchJsonWithBearer(String path, Map<String, ?> body, String accessToken) {
        return exchangeJson(HttpMethod.PATCH, path, body, accessToken);
    }

    protected ResponseEntity<String> putJsonWithBearer(String path, Map<String, ?> body, String accessToken) {
        return exchangeJson(HttpMethod.PUT, path, body, accessToken);
    }

    protected ResponseEntity<String> deleteWithBearer(String path, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return rest.exchange(path, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<String> exchangeJson(HttpMethod method, String path, Map<String, ?> body,
            String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            return rest.exchange(path, method, new HttpEntity<>(jsonBody, headers), String.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalArgumentException("Could not serialise test request body", ex);
        }
    }
}
