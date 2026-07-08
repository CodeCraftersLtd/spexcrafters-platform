package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Invitation and membership lifecycle per organizations-capability-model.md §§2–4:
 * identity-bound single-use tokens, rank rules, removal, the last-owner invariant and the
 * module-owned audit trail. Raw tokens are always captured from the recorded email —
 * never from the database, which stores only the SHA-256 hash.
 */
class MembershipLifecycleIntegrationTest extends AbstractOrganizationsIntegrationTest {

    @Test
    void invitationAcceptFlowAndTokenReplay() {
        TestUser owner = signUpUser();
        TestUser invitee = signUpUser();
        String orgId = createOrganization(owner, "Lifecycle Optics");

        // Invite: 201, PENDING, no token material in the response.
        ResponseEntity<String> created = postJsonWithBearer(
                "/api/v1/organizations/" + orgId + "/invitations",
                Map.of("email", invitee.email(), "role", "MEMBER"), owner.accessToken());
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode invitation = json(created);
        assertThat(invitation.get("email").asText()).isEqualTo(invitee.email());
        assertThat(invitation.get("role").asText()).isEqualTo("MEMBER");
        assertThat(invitation.get("status").asText()).isEqualTo("PENDING");
        assertThat(invitation.get("expiresAt").asText()).isNotBlank();
        assertThat(invitation.has("token")).isFalse();
        assertThat(invitation.has("tokenHash")).isFalse();

        String rawToken = awaitInvitationToken(invitee.email());

        // Accept by the matching user: 200 MyMembership.
        ResponseEntity<String> accepted = postJsonWithBearer("/api/v1/invitations/accept",
                Map.of("token", rawToken), invitee.accessToken());
        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode membership = json(accepted);
        assertThat(membership.get("organizationId").asText()).isEqualTo(orgId);
        assertThat(membership.get("organizationName").asText()).isEqualTo("Lifecycle Optics");
        assertThat(membership.get("role").asText()).isEqualTo("MEMBER");

        // The new member is listed alongside the owner.
        ResponseEntity<String> members = getWithBearer(
                "/api/v1/organizations/" + orgId + "/members", owner.accessToken());
        assertThat(json(members)).hasSize(2);

        // The invitation shows as ACCEPTED.
        ResponseEntity<String> invitations = getWithBearer(
                "/api/v1/organizations/" + orgId + "/invitations", owner.accessToken());
        assertThat(json(invitations).get(0).get("status").asText()).isEqualTo("ACCEPTED");

        // Token replay: 410.
        ResponseEntity<String> replay = postJsonWithBearer("/api/v1/invitations/accept",
                Map.of("token", rawToken), invitee.accessToken());
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.GONE);

        // Audit trail.
        assertThat(countAuditRows("organization.invitation.created", owner.userId())).isEqualTo(1);
        assertThat(countAuditRows("organization.invitation.accepted", invitee.userId())).isEqualTo(1);
    }

    @Test
    void acceptIsBoundToTheInvitedEmail() {
        TestUser owner = signUpUser();
        TestUser stranger = signUpUser();
        String orgId = createOrganization(owner, "Identity Bound Optics");

        String invitedEmail = uniqueEmail();
        String rawToken = inviteAndCaptureToken(owner, orgId, invitedEmail, "MEMBER");

        // A user with a different account email: 403 identity mismatch, no org details.
        ResponseEntity<String> mismatch = postJsonWithBearer("/api/v1/invitations/accept",
                Map.of("token", rawToken), stranger.accessToken());
        assertThat(mismatch.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        JsonNode problem = json(mismatch);
        assertThat(problem.get("type").asText()).endsWith("/problems/invitation-identity-mismatch");
        assertThat(problem.toString()).doesNotContain(orgId).doesNotContain("Identity Bound Optics");

        // The token stays usable for the rightful account.
        ResponseEntity<String> invitations = getWithBearer(
                "/api/v1/organizations/" + orgId + "/invitations", owner.accessToken());
        assertThat(json(invitations).get(0).get("status").asText()).isEqualTo("PENDING");
        String invitationId = json(invitations).get(0).get("id").asText();

        // The stranger did not join anything.
        assertThat(json(getWithBearer("/api/v1/me/organizations", stranger.accessToken()))).isEmpty();

        // TD-10: the mismatch survives the 403 rollback as an audit row whose jsonb detail
        // carries ids only — never email addresses.
        assertThat(countAuditRows("organization.invitation.mismatch", stranger.userId())).isEqualTo(1);
        assertThat(auditDetails("organization.invitation.mismatch", stranger.userId()))
                .singleElement().asString()
                .contains("\"invitationId\":\"" + invitationId + "\"")
                .contains("\"actorUserId\":\"" + stranger.userId() + "\"")
                .doesNotContain("@")
                .doesNotContain(invitedEmail)
                .doesNotContain(stranger.email());
    }

    @Test
    void revokedInvitationIsGoneButRevokeIsIdempotent() {
        TestUser owner = signUpUser();
        TestUser invitee = signUpUser();
        String orgId = createOrganization(owner, "Revocation Optics");

        ResponseEntity<String> created = postJsonWithBearer(
                "/api/v1/organizations/" + orgId + "/invitations",
                Map.of("email", invitee.email(), "role", "MEMBER"), owner.accessToken());
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String invitationId = json(created).get("id").asText();
        String rawToken = awaitInvitationToken(invitee.email());

        // Revoke: 204; revoking again is idempotent: 204.
        String revokePath = "/api/v1/organizations/" + orgId + "/invitations/" + invitationId + "/revoke";
        assertThat(postJsonWithBearer(revokePath, Map.of(), owner.accessToken()).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(postJsonWithBearer(revokePath, Map.of(), owner.accessToken()).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        // Accepting a revoked token: 410.
        ResponseEntity<String> accept = postJsonWithBearer("/api/v1/invitations/accept",
                Map.of("token", rawToken), invitee.accessToken());
        assertThat(accept.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(json(accept).get("type").asText()).endsWith("/problems/token-gone");

        // Exactly one revocation was audited (the idempotent re-submit records nothing).
        assertThat(countAuditRows("organization.invitation.revoked", owner.userId())).isEqualTo(1);
    }

    @Test
    void duplicateMembershipAndPendingInvitationConflicts() {
        TestUser owner = signUpUser();
        TestUser member = signUpUser();
        String orgId = createOrganization(owner, "Duplicate Optics");

        String rawToken = inviteAndCaptureToken(owner, orgId, member.email(), "MEMBER");
        assertThat(postJsonWithBearer("/api/v1/invitations/accept",
                Map.of("token", rawToken), member.accessToken()).getStatusCode()).isEqualTo(HttpStatus.OK);

        // Inviting an existing ACTIVE member: 409 duplicate-membership.
        ResponseEntity<String> duplicateMember = postJsonWithBearer(
                "/api/v1/organizations/" + orgId + "/invitations",
                Map.of("email", member.email(), "role", "MEMBER"), owner.accessToken());
        assertThat(duplicateMember.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(json(duplicateMember).get("type").asText()).endsWith("/problems/duplicate-membership");

        // A second PENDING invitation for the same email: 409.
        String pendingEmail = uniqueEmail();
        inviteAndCaptureToken(owner, orgId, pendingEmail, "MEMBER");
        ResponseEntity<String> duplicatePending = postJsonWithBearer(
                "/api/v1/organizations/" + orgId + "/invitations",
                Map.of("email", pendingEmail.toUpperCase(), "role", "MEMBER"), owner.accessToken());
        assertThat(duplicatePending.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // OWNER cannot be invited: 422 with a field error on role.
        ResponseEntity<String> ownerInvite = postJsonWithBearer(
                "/api/v1/organizations/" + orgId + "/invitations",
                Map.of("email", uniqueEmail(), "role", "OWNER"), owner.accessToken());
        assertThat(ownerInvite.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(ownerInvite).get("errors").get(0).get("field").asText()).isEqualTo("role");
    }

    @Test
    void rankRulesPreventPrivilegeEscalation() {
        TestUser owner = signUpUser();
        TestUser admin = signUpUser();
        TestUser admin2 = signUpUser();
        TestUser member = signUpUser();
        String orgId = createOrganization(owner, "Rank Rule Optics");

        joinAs(owner, orgId, admin, "ADMIN");
        joinAs(owner, orgId, admin2, "ADMIN");
        joinAs(owner, orgId, member, "MEMBER");

        // ADMIN can invite MEMBER-role targets...
        ResponseEntity<String> adminInvitesMember = postJsonWithBearer(
                "/api/v1/organizations/" + orgId + "/invitations",
                Map.of("email", uniqueEmail(), "role", "MEMBER"), admin.accessToken());
        assertThat(adminInvitesMember.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // ...but not ADMIN-role targets: 403 authorization problem.
        ResponseEntity<String> adminInvitesAdmin = postJsonWithBearer(
                "/api/v1/organizations/" + orgId + "/invitations",
                Map.of("email", uniqueEmail(), "role", "ADMIN"), admin.accessToken());
        assertThat(adminInvitesAdmin.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(json(adminInvitesAdmin).get("type").asText()).endsWith("/problems/authorization");

        // MEMBER cannot invite at all: 403.
        ResponseEntity<String> memberInvites = postJsonWithBearer(
                "/api/v1/organizations/" + orgId + "/invitations",
                Map.of("email", uniqueEmail(), "role", "MEMBER"), member.accessToken());
        assertThat(memberInvites.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // ADMIN cannot change roles (roles.manage is OWNER-only): 403.
        String memberMembershipId = membershipIdOf(owner, orgId, member.userId());
        ResponseEntity<String> adminChangesRole = putJsonWithBearer(
                "/api/v1/organizations/" + orgId + "/members/" + memberMembershipId + "/role",
                Map.of("role", "ADMIN"), admin.accessToken());
        assertThat(adminChangesRole.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(json(adminChangesRole).get("type").asText()).endsWith("/problems/authorization");

        // ADMIN cannot remove a peer ADMIN (not strictly lower rank): 403.
        String admin2MembershipId = membershipIdOf(owner, orgId, admin2.userId());
        assertThat(deleteWithBearer("/api/v1/organizations/" + orgId + "/members/" + admin2MembershipId,
                admin.accessToken()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // ADMIN cannot remove the OWNER either: 403.
        String ownerMembershipId = membershipIdOf(owner, orgId, owner.userId());
        assertThat(deleteWithBearer("/api/v1/organizations/" + orgId + "/members/" + ownerMembershipId,
                admin.accessToken()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // ADMIN may remove a MEMBER (strictly lower rank): 204.
        assertThat(deleteWithBearer("/api/v1/organizations/" + orgId + "/members/" + memberMembershipId,
                admin.accessToken()).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Every denied attempt above was audited.
        assertThat(countAuditRows("authorization.denied", admin.userId())).isGreaterThanOrEqualTo(4);
        assertThat(countAuditRows("authorization.denied", member.userId())).isGreaterThanOrEqualTo(1);
    }

    @Test
    void removedMembersLoseAllAccess() {
        TestUser owner = signUpUser();
        TestUser member = signUpUser();
        String orgId = createOrganization(owner, "Removal Optics");
        joinAs(owner, orgId, member, "MEMBER");

        String membershipId = membershipIdOf(owner, orgId, member.userId());
        assertThat(deleteWithBearer("/api/v1/organizations/" + orgId + "/members/" + membershipId,
                owner.accessToken()).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(countAuditRows("organization.member.removed", owner.userId())).isEqualTo(1);

        // The removed (inactive) member behaves like a non-member: concealed 404.
        ResponseEntity<String> get = getWithBearer("/api/v1/organizations/" + orgId, member.accessToken());
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(json(get).get("type").asText()).endsWith("/problems/not-found");
        assertThat(getWithBearer("/api/v1/organizations/" + orgId + "/members", member.accessToken())
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(json(getWithBearer("/api/v1/me/organizations", member.accessToken()))).isEmpty();

        // Deleting the already-removed membership again: 404 (row no longer ACTIVE).
        assertThat(deleteWithBearer("/api/v1/organizations/" + orgId + "/members/" + membershipId,
                owner.accessToken()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void lastOwnerInvariantAndOwnershipTransition() {
        TestUser owner = signUpUser();
        TestUser successor = signUpUser();
        String orgId = createOrganization(owner, "Succession Optics");
        joinAs(owner, orgId, successor, "MEMBER");

        String ownerMembershipId = membershipIdOf(owner, orgId, owner.userId());
        String successorMembershipId = membershipIdOf(owner, orgId, successor.userId());

        // The sole OWNER cannot leave: 409 last-owner.
        ResponseEntity<String> selfRemove = deleteWithBearer(
                "/api/v1/organizations/" + orgId + "/members/" + ownerMembershipId, owner.accessToken());
        assertThat(selfRemove.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(json(selfRemove).get("type").asText()).endsWith("/problems/last-owner");

        // Self role-change is forbidden — a sole OWNER cannot demote themselves either:
        // 409 invalid-role-change (self-change is rejected before any owner counting).
        ResponseEntity<String> selfDemote = putJsonWithBearer(
                "/api/v1/organizations/" + orgId + "/members/" + ownerMembershipId + "/role",
                Map.of("role", "MEMBER"), owner.accessToken());
        assertThat(selfDemote.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(json(selfDemote).get("type").asText()).endsWith("/problems/invalid-role-change");

        // Ownership transition: promote a second OWNER (OWNER-only roles.manage)...
        ResponseEntity<String> promote = putJsonWithBearer(
                "/api/v1/organizations/" + orgId + "/members/" + successorMembershipId + "/role",
                Map.of("role", "OWNER"), owner.accessToken());
        assertThat(promote.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(promote).get("role").asText()).isEqualTo("OWNER");
        assertThat(countAuditRows("organization.member.role_changed", owner.userId())).isEqualTo(1);

        // ...then the original owner may leave: 204.
        assertThat(deleteWithBearer("/api/v1/organizations/" + orgId + "/members/" + ownerMembershipId,
                owner.accessToken()).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // And the new sole OWNER is protected by the invariant in turn.
        ResponseEntity<String> successorLeaves = deleteWithBearer(
                "/api/v1/organizations/" + orgId + "/members/" + successorMembershipId,
                successor.accessToken());
        assertThat(successorLeaves.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(json(successorLeaves).get("type").asText()).endsWith("/problems/last-owner");

        // Non-owner self-removal (leave) is always permitted: re-join as MEMBER and leave.
        joinAs(successor, orgId, owner, "MEMBER");
        String rejoinedMembershipId = membershipIdOf(successor, orgId, owner.userId());
        assertThat(rejoinedMembershipId).isNotEqualTo(ownerMembershipId); // re-join = new row
        assertThat(deleteWithBearer("/api/v1/organizations/" + orgId + "/members/" + rejoinedMembershipId,
                owner.accessToken()).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    /** Invites {@code joiner} with {@code role} (as {@code inviter}) and accepts the invitation. */
    private void joinAs(TestUser inviter, String organizationId, TestUser joiner, String role) {
        String rawToken = inviteAndCaptureToken(inviter, organizationId, joiner.email(), role);
        ResponseEntity<String> accepted = postJsonWithBearer("/api/v1/invitations/accept",
                Map.of("token", rawToken), joiner.accessToken());
        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
