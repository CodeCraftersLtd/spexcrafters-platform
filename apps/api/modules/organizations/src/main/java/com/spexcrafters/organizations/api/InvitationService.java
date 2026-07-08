package com.spexcrafters.organizations.api;

import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.identity.api.UserDirectory;
import com.spexcrafters.identity.api.UserSummaryDto;
import com.spexcrafters.organizations.domain.Capability;
import com.spexcrafters.organizations.domain.DuplicateMembershipException;
import com.spexcrafters.organizations.domain.InvitationIdentityMismatchException;
import com.spexcrafters.organizations.domain.InvitationStatus;
import com.spexcrafters.organizations.domain.InvitationToken;
import com.spexcrafters.organizations.domain.MembershipStatus;
import com.spexcrafters.organizations.domain.Organization;
import com.spexcrafters.organizations.domain.OrganizationInvitation;
import com.spexcrafters.organizations.domain.OrganizationMembership;
import com.spexcrafters.organizations.domain.OrganizationNotFoundException;
import com.spexcrafters.organizations.domain.OrganizationRole;
import com.spexcrafters.organizations.infrastructure.OrganizationInvitationRepository;
import com.spexcrafters.organizations.infrastructure.OrganizationMembershipRepository;
import com.spexcrafters.organizations.infrastructure.OrganizationRepository;
import com.spexcrafters.organizations.infrastructure.config.InvitationProperties;
import com.spexcrafters.organizations.infrastructure.mail.InvitationMailer;
import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import com.spexcrafters.sharedkernel.problem.AuthenticationFailedException;
import com.spexcrafters.sharedkernel.problem.ConflictException;
import com.spexcrafters.sharedkernel.problem.ProblemFieldError;
import com.spexcrafters.sharedkernel.problem.ResourceGoneException;
import com.spexcrafters.sharedkernel.util.UuidV7;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Invitation lifecycle per organizations-capability-model.md §4: single-use 7-day tokens
 * (SHA-256 hash stored, raw token delivered solely via email), identity-bound acceptance,
 * idempotent revocation and uniform 410 for consumed/revoked/expired/unknown tokens so
 * token state cannot be probed.
 */
@Service
public class InvitationService {

    private static final String GONE_DETAIL =
            "This invitation is invalid, expired, revoked or has already been used.";

    private final OrganizationRepository organizations;
    private final OrganizationMembershipRepository memberships;
    private final OrganizationInvitationRepository invitations;
    private final OrganizationAccess access;
    private final UserDirectory userDirectory;
    private final InvitationMailer invitationMailer;
    private final InvitationExpirer invitationExpirer;
    private final AuditLogger auditLogger;
    private final InvitationProperties invitationProperties;
    private final Clock clock;

    public InvitationService(OrganizationRepository organizations,
            OrganizationMembershipRepository memberships,
            OrganizationInvitationRepository invitations,
            OrganizationAccess access,
            UserDirectory userDirectory,
            InvitationMailer invitationMailer,
            InvitationExpirer invitationExpirer,
            AuditLogger auditLogger,
            InvitationProperties invitationProperties,
            Clock clock) {
        this.organizations = organizations;
        this.memberships = memberships;
        this.invitations = invitations;
        this.access = access;
        this.userDirectory = userDirectory;
        this.invitationMailer = invitationMailer;
        this.invitationExpirer = invitationExpirer;
        this.auditLogger = auditLogger;
        this.invitationProperties = invitationProperties;
        this.clock = clock;
    }

    /**
     * Creates an invitation and dispatches the email. OWNER cannot be invited (422);
     * ADMIN actors may only set role MEMBER (rank rule, 403); an existing ACTIVE
     * membership for the email's account (409 {@code duplicate-membership}) or a PENDING
     * invitation for the same email (409) rejects the request.
     */
    @Transactional
    public InvitationDto create(UUID actorUserId, UUID organizationId, String rawEmail, OrganizationRole role) {
        OrganizationContext context = access.require(actorUserId, organizationId, Capability.MEMBERS_INVITE);
        if (role == OrganizationRole.OWNER) {
            throw ApiProblemException.validation(List.of(new ProblemFieldError(
                    "role", "role.owner_not_invitable", "OWNER cannot be invited; promote the member after joining.")));
        }
        if (context.role() != OrganizationRole.OWNER && !context.role().higherThan(role)) {
            // Rank rule: an actor may never invite to a role >= their own unless OWNER.
            throw access.deny(actorUserId, organizationId, Capability.MEMBERS_INVITE.wireName());
        }

        String email = normalizeEmail(rawEmail);
        userDirectory.findByEmail(email).ifPresent(existingUser -> {
            if (memberships.existsByOrganizationIdAndUserIdAndStatus(
                    organizationId, existingUser.id(), MembershipStatus.ACTIVE)) {
                throw new DuplicateMembershipException();
            }
        });
        if (invitations.existsByOrganizationIdAndEmailAndStatus(organizationId, email, InvitationStatus.PENDING)) {
            // Backed by the partial unique index; a concurrent race surfaces as a
            // DataIntegrityViolationException which the global handler also maps to 409.
            throw new ConflictException("A pending invitation for this email address already exists.");
        }

        String rawToken = InvitationToken.generate();
        OrganizationInvitation invitation = new OrganizationInvitation(
                UuidV7.generate(),
                organizationId,
                email,
                role,
                InvitationToken.sha256Hex(rawToken),
                actorUserId,
                clock.instant().plus(invitationProperties.invitationTtl()));
        invitation.setCreatedBy(actorUserId);
        invitation.setUpdatedBy(actorUserId);
        invitations.save(invitation);

        invitationMailer.sendInvitationEmail(email, context.organization().getName(), role, rawToken);
        auditLogger.record("organization.invitation.created", actorUserId,
                "organization_invitation", invitation.getId().toString());
        return toDto(invitation);
    }

    @Transactional(readOnly = true)
    public List<InvitationDto> list(UUID userId, UUID organizationId) {
        access.require(userId, organizationId, Capability.MEMBERS_READ);
        return invitations.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(InvitationService::toDto)
                .toList();
    }

    /**
     * Revokes a PENDING invitation. Idempotent for an already-REVOKED invitation (204);
     * an ACCEPTED or EXPIRED invitation is no longer revocable (410).
     */
    @Transactional
    public void revoke(UUID actorUserId, UUID organizationId, UUID invitationId) {
        access.require(actorUserId, organizationId, Capability.MEMBERS_INVITE);
        OrganizationInvitation invitation = invitations.findByIdAndOrganizationId(invitationId, organizationId)
                .orElseThrow(OrganizationNotFoundException::new);
        switch (invitation.getStatus()) {
            case REVOKED -> {
                // Idempotent re-submit.
            }
            case ACCEPTED, EXPIRED -> throw new ResourceGoneException(GONE_DETAIL);
            case PENDING -> {
                invitation.markRevoked();
                invitation.setUpdatedBy(actorUserId);
                auditLogger.record("organization.invitation.revoked", actorUserId,
                        "organization_invitation", invitation.getId().toString());
            }
        }
    }

    /**
     * Accepts an invitation token for the authenticated user. The account email must equal
     * the invitation email case-insensitively (403 {@code invitation-identity-mismatch},
     * no organization details leaked); a past-expiry PENDING token is lazily marked
     * EXPIRED (surviving the 410 rollback); membership creation and token consumption are
     * atomic.
     */
    @Transactional
    public MyMembershipDto accept(UUID userId, String rawToken) {
        OrganizationInvitation invitation = invitations.findByTokenHash(InvitationToken.sha256Hex(rawToken))
                .orElseThrow(() -> new ResourceGoneException(GONE_DETAIL));
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ResourceGoneException(GONE_DETAIL);
        }
        Instant now = clock.instant();
        if (invitation.isExpired(now)) {
            invitationExpirer.markExpired(invitation.getId());
            throw new ResourceGoneException(GONE_DETAIL);
        }

        UserSummaryDto caller = userDirectory.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("This account no longer exists."));
        if (!caller.email().equalsIgnoreCase(invitation.getEmail())) {
            throw new InvitationIdentityMismatchException();
        }
        if (memberships.existsByOrganizationIdAndUserIdAndStatus(
                invitation.getOrganizationId(), userId, MembershipStatus.ACTIVE)) {
            throw new DuplicateMembershipException();
        }

        invitation.markAccepted(now, userId);
        invitation.setUpdatedBy(userId);

        OrganizationMembership membership = new OrganizationMembership(
                UuidV7.generate(), invitation.getOrganizationId(), userId, invitation.getRole(), now);
        membership.setCreatedBy(userId);
        membership.setUpdatedBy(userId);
        memberships.save(membership);

        Organization organization = organizations.findById(invitation.getOrganizationId())
                .orElseThrow(() -> new IllegalStateException(
                        "Invitation " + invitation.getId() + " references a missing organization"));
        auditLogger.record("organization.invitation.accepted", userId,
                "organization_invitation", invitation.getId().toString());

        return new MyMembershipDto(
                membership.getId(),
                organization.getId(),
                organization.getName(),
                organization.getType(),
                membership.getRole(),
                membership.getJoinedAt());
    }

    private static InvitationDto toDto(OrganizationInvitation invitation) {
        return new InvitationDto(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
