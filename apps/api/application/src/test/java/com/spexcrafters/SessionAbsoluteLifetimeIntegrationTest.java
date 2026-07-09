package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/**
 * Absolute session lifetime (session-security-policy.md §1): once a refresh-token family
 * is older than {@code spexcrafters.auth.session-absolute-ttl}, renewal is denied (401)
 * regardless of activity — audited as {@code identity.session.expired_absolute}, without
 * revocation-audit spam and without treating the legitimate user as a thief.
 *
 * <p>The cap is shortened to 3 s so the test outlives it with a small sleep instead of 30 days.
 */
@TestPropertySource(properties = {
        "spexcrafters.auth.session-absolute-ttl=3s",
        "spexcrafters.auth.refresh-grace=1s"
})
class SessionAbsoluteLifetimeIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "sturdy-passphrase-42";
    /** Keep in sync with the {@code @TestPropertySource} above. */
    private static final Duration ABSOLUTE_TTL = Duration.ofSeconds(3);

    @Test
    void renewalIsDeniedOnceTheFamilyOutlivesTheAbsoluteCap() throws InterruptedException {
        String email = uniqueEmail();
        registerAndVerify(email, PASSWORD);
        JsonNode session = login(email, PASSWORD);
        String userId = session.get("user").get("id").asText();
        String firstToken = session.get("refreshToken").asText();

        // Inside the cap the family renews normally.
        ResponseEntity<String> withinCap = postJson("/api/v1/auth/refresh",
                Map.of("refreshToken", firstToken));
        assertThat(withinCap.getStatusCode()).isEqualTo(HttpStatus.OK);
        String secondToken = json(withinCap).get("refreshToken").asText();

        // Outlive the absolute lifetime (measured from the family's first token = login) —
        // activity does not extend it.
        Thread.sleep(ABSOLUTE_TTL.plusMillis(500).toMillis());
        ResponseEntity<String> denied = postJson("/api/v1/auth/refresh",
                Map.of("refreshToken", secondToken));
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(json(denied).get("type").asText()).endsWith("/problems/authentication-failed");
        assertThat(json(denied).get("detail").asText()).containsIgnoringCase("absolute lifetime");

        // Audited exactly once, with the family id in the jsonb detail.
        String familyId = familyIdOf(userId);
        assertThat(auditDetails("identity.session.expired_absolute", userId))
                .singleElement().asString().contains("\"familyId\"").contains(familyId);

        // The legitimate user is not treated as a thief: no replay/revocation events and
        // the family rows were left unrevoked (they simply can no longer be renewed).
        assertThat(countAuditRows("identity.session.replay_detected", userId)).isZero();
        assertThat(countAuditRows("identity.session.family_revoked", userId)).isZero();
        Long revokedRows = jdbcTemplate.queryForObject(
                "select count(*) from identity.refresh_token where user_id = ? and revoked_at is not null",
                Long.class, UUID.fromString(userId));
        assertThat(revokedRows).isZero();
    }

    /** The single refresh-token family id of {@code userId} (one login in this test). */
    private String familyIdOf(String userId) {
        return jdbcTemplate.queryForObject(
                "select distinct family_id::text from identity.refresh_token where user_id = ?",
                String.class, UUID.fromString(userId));
    }
}
