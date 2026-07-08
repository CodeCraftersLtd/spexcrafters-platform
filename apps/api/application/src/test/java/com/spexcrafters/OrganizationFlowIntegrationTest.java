package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Happy path of the organization resource: create (creator becomes OWNER atomically with
 * full capabilities) → read → optimistic update → stale-version conflict, plus the
 * module-owned audit trail.
 */
class OrganizationFlowIntegrationTest extends AbstractOrganizationsIntegrationTest {

    private static final List<String> ALL_CAPABILITIES = List.of(
            "organization.read",
            "organization.update",
            "organization.members.read",
            "organization.members.invite",
            "organization.members.remove",
            "organization.roles.manage");

    @Test
    void createReadUpdateFlowWithOptimisticLocking() {
        TestUser owner = signUpUser();

        // Create: 201 with the caller's role and resolved capabilities.
        ResponseEntity<String> created = postJsonWithBearer("/api/v1/organizations",
                Map.of("name", "Acme Optics", "type", "SUPPLIER", "country", "DE"),
                owner.accessToken());
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode organization = json(created);
        String organizationId = organization.get("id").asText();
        assertThat(organization.get("name").asText()).isEqualTo("Acme Optics");
        assertThat(organization.get("type").asText()).isEqualTo("SUPPLIER");
        assertThat(organization.get("country").asText()).isEqualTo("DE");
        assertThat(organization.get("version").asInt()).isZero();
        assertThat(organization.get("callerRole").asText()).isEqualTo("OWNER");
        assertThat(capabilities(organization)).containsExactlyInAnyOrderElementsOf(ALL_CAPABILITIES);

        // The creator's OWNER membership exists atomically with the organization.
        ResponseEntity<String> myOrganizations = getWithBearer("/api/v1/me/organizations", owner.accessToken());
        assertThat(myOrganizations.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode memberships = json(myOrganizations);
        assertThat(memberships).hasSize(1);
        assertThat(memberships.get(0).get("organizationId").asText()).isEqualTo(organizationId);
        assertThat(memberships.get(0).get("organizationName").asText()).isEqualTo("Acme Optics");
        assertThat(memberships.get(0).get("organizationType").asText()).isEqualTo("SUPPLIER");
        assertThat(memberships.get(0).get("role").asText()).isEqualTo("OWNER");
        assertThat(memberships.get(0).get("joinedAt").asText()).isNotBlank();

        // Get: 200 with the same representation.
        ResponseEntity<String> fetched = getWithBearer("/api/v1/organizations/" + organizationId,
                owner.accessToken());
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(fetched).get("version").asInt()).isZero();

        // The creator lists as the only ACTIVE member, with identity-provided details.
        ResponseEntity<String> members = getWithBearer(
                "/api/v1/organizations/" + organizationId + "/members", owner.accessToken());
        assertThat(members.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode memberList = json(members);
        assertThat(memberList).hasSize(1);
        assertThat(memberList.get(0).get("userId").asText()).isEqualTo(owner.userId());
        assertThat(memberList.get(0).get("email").asText()).isEqualTo(owner.email());
        assertThat(memberList.get(0).get("displayName").asText()).isEqualTo("Integration Tester");
        assertThat(memberList.get(0).get("role").asText()).isEqualTo("OWNER");

        // Update with the current version: 200, version bumped.
        ResponseEntity<String> updated = patchJsonWithBearer("/api/v1/organizations/" + organizationId,
                Map.of("name", "Acme Optics International", "version", 0), owner.accessToken());
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(updated).get("name").asText()).isEqualTo("Acme Optics International");
        assertThat(json(updated).get("version").asInt()).isEqualTo(1);
        assertThat(json(updated).get("country").asText()).isEqualTo("DE"); // untouched field kept

        // Update with the stale version: 409 conflict.
        ResponseEntity<String> stale = patchJsonWithBearer("/api/v1/organizations/" + organizationId,
                Map.of("name", "Stale Write", "version", 0), owner.accessToken());
        assertThat(stale.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(stale.getHeaders().getContentType()).hasToString("application/problem+json");
        assertThat(json(stale).get("type").asText()).endsWith("/problems/conflict");

        // The organization is unchanged after the rejected write.
        ResponseEntity<String> afterConflict = getWithBearer("/api/v1/organizations/" + organizationId,
                owner.accessToken());
        assertThat(json(afterConflict).get("name").asText()).isEqualTo("Acme Optics International");

        // Module-owned audit trail.
        assertThat(countAuditRows("organization.created", owner.userId())).isEqualTo(1);
        assertThat(countAuditRows("organization.updated", owner.userId())).isEqualTo(1);
    }

    @Test
    void createValidatesTheRequest() {
        TestUser user = signUpUser();

        // Name too short → 422 validation problem.
        ResponseEntity<String> shortName = postJsonWithBearer("/api/v1/organizations",
                Map.of("name", "A", "type", "BUYER"), user.accessToken());
        assertThat(shortName.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(shortName).get("type").asText()).endsWith("/problems/validation-error");

        // Unknown organization type → 422 (unreadable body).
        ResponseEntity<String> badType = postJsonWithBearer("/api/v1/organizations",
                Map.of("name", "Acme Optics", "type", "WHOLESALER"), user.accessToken());
        assertThat(badType.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void organizationEndpointsRequireAuthentication() {
        ResponseEntity<String> response = rest.postForEntity("/api/v1/organizations", null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private static List<String> capabilities(JsonNode organization) {
        List<String> result = new ArrayList<>();
        organization.get("callerCapabilities").forEach(node -> result.add(node.asText()));
        return result;
    }
}
