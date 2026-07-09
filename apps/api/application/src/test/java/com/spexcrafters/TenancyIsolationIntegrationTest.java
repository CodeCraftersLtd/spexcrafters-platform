package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Tenancy concealment (organizations-capability-model.md §5): non-members receive an
 * indistinguishable 404 ({@code not-found} problem) for every organization-scoped
 * resource — read, write, member and role operations, in both directions — and each
 * concealed denial leaves an {@code authorization.denied} audit row.
 */
class TenancyIsolationIntegrationTest extends AbstractOrganizationsIntegrationTest {

    @Test
    void nonMembersReceiveConcealed404sInBothDirections() {
        TestUser userA = signUpUser();
        TestUser userB = signUpUser();
        String orgA = createOrganization(userA, "Org A Optics");
        String orgB = createOrganization(userB, "Org B Optics");
        String membershipAOfA = membershipIdOf(userA, orgA, userA.userId());
        String membershipBOfB = membershipIdOf(userB, orgB, userB.userId());

        // --- B probing A's organization ---

        // GET organization → 404, not 403 (existence concealment).
        ResponseEntity<String> get = getWithBearer("/api/v1/organizations/" + orgA, userB.accessToken());
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get.getHeaders().getContentType()).hasToString("application/problem+json");
        assertThat(json(get).get("type").asText()).endsWith("/problems/not-found");

        // PATCH organization → 404.
        ResponseEntity<String> patch = patchJsonWithBearer("/api/v1/organizations/" + orgA,
                Map.of("name", "Hijacked", "version", 0), userB.accessToken());
        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // List members → 404.
        ResponseEntity<String> members = getWithBearer(
                "/api/v1/organizations/" + orgA + "/members", userB.accessToken());
        assertThat(members.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // List/create invitations → 404.
        ResponseEntity<String> invitations = getWithBearer(
                "/api/v1/organizations/" + orgA + "/invitations", userB.accessToken());
        assertThat(invitations.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ResponseEntity<String> invite = postJsonWithBearer(
                "/api/v1/organizations/" + orgA + "/invitations",
                Map.of("email", uniqueEmail(), "role", "MEMBER"), userB.accessToken());
        assertThat(invite.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Remove A's member → 404 (cannot even see the membership).
        ResponseEntity<String> remove = deleteWithBearer(
                "/api/v1/organizations/" + orgA + "/members/" + membershipAOfA, userB.accessToken());
        assertThat(remove.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Change roles in A → 404.
        ResponseEntity<String> changeRole = putJsonWithBearer(
                "/api/v1/organizations/" + orgA + "/members/" + membershipAOfA + "/role",
                Map.of("role", "MEMBER"), userB.accessToken());
        assertThat(changeRole.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // --- A probing B's organization (the other direction) ---

        assertThat(getWithBearer("/api/v1/organizations/" + orgB, userA.accessToken())
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(patchJsonWithBearer("/api/v1/organizations/" + orgB,
                Map.of("name", "Hijacked", "version", 0), userA.accessToken())
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getWithBearer("/api/v1/organizations/" + orgB + "/members", userA.accessToken())
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(deleteWithBearer("/api/v1/organizations/" + orgB + "/members/" + membershipBOfB,
                userA.accessToken()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(putJsonWithBearer("/api/v1/organizations/" + orgB + "/members/" + membershipBOfB + "/role",
                Map.of("role", "ADMIN"), userA.accessToken()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // A completely unknown organization id is indistinguishable from a concealed one.
        ResponseEntity<String> unknown = getWithBearer(
                "/api/v1/organizations/00000000-0000-7000-8000-000000000000", userB.accessToken());
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(json(unknown).get("type").asText()).endsWith("/problems/not-found");

        // Concealed denials are audited (rolled-back business tx notwithstanding).
        assertThat(countAuditRows("authorization.denied", userB.userId())).isGreaterThanOrEqualTo(7);
        assertThat(countAuditRows("authorization.denied", userA.userId())).isGreaterThanOrEqualTo(5);

        // TD-9: target_id is the plain organization id (no "capability=" encoding) and the
        // checked capability travels in the structured jsonb detail instead.
        assertThat(auditTargetIds("authorization.denied", userB.userId()))
                .contains(orgA)
                .allSatisfy(targetId -> assertThat(targetId).doesNotContain("capability="));
        assertThat(auditDetails("authorization.denied", userB.userId()))
                .isNotEmpty()
                .allSatisfy(detail -> assertThat(detail)
                        .contains("\"capability\":\"organization.")
                        .contains("\"organizationId\":\""))
                .anySatisfy(detail -> assertThat(detail)
                        .contains("\"capability\":\"organization.read\"")
                        .contains(orgA));

        // Nothing leaked or changed: A still sees its organization untouched.
        ResponseEntity<String> intact = getWithBearer("/api/v1/organizations/" + orgA, userA.accessToken());
        assertThat(intact.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(intact).get("name").asText()).isEqualTo("Org A Optics");
        assertThat(json(intact).get("version").asInt()).isZero();
    }
}
