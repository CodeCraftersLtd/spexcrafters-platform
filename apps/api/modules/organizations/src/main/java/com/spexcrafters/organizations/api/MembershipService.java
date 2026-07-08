package com.spexcrafters.organizations.api;

import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.identity.api.UserDirectory;
import com.spexcrafters.identity.api.UserSummaryDto;
import com.spexcrafters.organizations.domain.Capability;
import com.spexcrafters.organizations.domain.InvalidRoleChangeException;
import com.spexcrafters.organizations.domain.LastOwnerException;
import com.spexcrafters.organizations.domain.MembershipStatus;
import com.spexcrafters.organizations.domain.OrganizationMembership;
import com.spexcrafters.organizations.domain.OrganizationNotFoundException;
import com.spexcrafters.organizations.domain.OrganizationRole;
import com.spexcrafters.organizations.infrastructure.OrganizationMembershipRepository;
import com.spexcrafters.organizations.infrastructure.OrganizationRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Membership listing, removal and role changes, enforcing the rank rules and the
 * last-owner invariant of organizations-capability-model.md §§2–3. Every owner-affecting
 * mutation first acquires a {@code PESSIMISTIC_WRITE} lock on the organization row, then
 * verifies the ACTIVE-OWNER count, serializing concurrent demotions/removals.
 */
@Service
public class MembershipService {

    private final OrganizationRepository organizations;
    private final OrganizationMembershipRepository memberships;
    private final OrganizationAccess access;
    private final UserDirectory userDirectory;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public MembershipService(OrganizationRepository organizations,
            OrganizationMembershipRepository memberships,
            OrganizationAccess access,
            UserDirectory userDirectory,
            AuditLogger auditLogger,
            Clock clock) {
        this.organizations = organizations;
        this.memberships = memberships;
        this.access = access;
        this.userDirectory = userDirectory;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    /**
     * Lists ACTIVE members with display name and email. User data comes from the identity
     * module's public {@link UserDirectory} — never from a cross-module table read.
     */
    @Transactional(readOnly = true)
    public List<MemberDto> listMembers(UUID userId, UUID organizationId) {
        access.require(userId, organizationId, Capability.MEMBERS_READ);
        List<OrganizationMembership> active =
                memberships.findByOrganizationIdAndStatusOrderByJoinedAtAsc(organizationId, MembershipStatus.ACTIVE);
        Map<UUID, UserSummaryDto> users = userDirectory
                .findSummariesByIds(active.stream().map(OrganizationMembership::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(UserSummaryDto::id, Function.identity()));
        return active.stream()
                .map(membership -> toDto(membership, users.get(membership.getUserId())))
                .toList();
    }

    /**
     * Removes a member. Self-removal (leave) is permitted for every role; removing someone
     * else requires {@code members.remove} plus the rank rule (OWNER may remove anyone,
     * ADMIN only strictly-lower ranks, i.e. MEMBER). Removing an OWNER — including
     * owner-leave — is guarded by the last-owner invariant under the organization-row lock.
     */
    @Transactional
    public void removeMember(UUID actorUserId, UUID organizationId, UUID membershipId) {
        OrganizationContext context = access.resolve(actorUserId, organizationId, Capability.MEMBERS_REMOVE);
        OrganizationMembership target = activeMembership(organizationId, membershipId);

        boolean selfRemoval = target.getId().equals(context.membership().getId());
        if (!selfRemoval) {
            if (!context.has(Capability.MEMBERS_REMOVE)) {
                throw access.deny(actorUserId, organizationId, Capability.MEMBERS_REMOVE.wireName());
            }
            if (context.role() != OrganizationRole.OWNER && !context.role().higherThan(target.getRole())) {
                throw access.deny(actorUserId, organizationId, Capability.MEMBERS_REMOVE.wireName());
            }
        }

        if (target.getRole() == OrganizationRole.OWNER) {
            ensureNotLastOwner(organizationId);
        }

        target.remove(clock.instant());
        target.setUpdatedBy(actorUserId);
        auditLogger.record("organization.member.removed", actorUserId,
                "organization_membership", target.getId().toString());
    }

    /**
     * Changes a member's role. {@code roles.manage} is OWNER-only; self role-change is
     * forbidden (409 {@code invalid-role-change}); demoting an OWNER goes through the same
     * last-owner lock as removal.
     */
    @Transactional
    public MemberDto changeRole(UUID actorUserId, UUID organizationId, UUID membershipId,
            OrganizationRole newRole) {
        access.require(actorUserId, organizationId, Capability.ROLES_MANAGE);
        OrganizationMembership target = activeMembership(organizationId, membershipId);

        if (target.getUserId().equals(actorUserId)) {
            throw new InvalidRoleChangeException(
                    "You cannot change your own role. Another owner must do it for you.");
        }
        if (target.getRole() == OrganizationRole.OWNER && newRole != OrganizationRole.OWNER) {
            ensureNotLastOwner(organizationId);
        }

        target.changeRole(newRole);
        target.setUpdatedBy(actorUserId);
        auditLogger.record("organization.member.role_changed", actorUserId,
                "organization_membership", target.getId().toString());

        UserSummaryDto user = userDirectory.findById(target.getUserId())
                .orElseThrow(() -> new IllegalStateException(
                        "Membership " + target.getId() + " references a missing user account"));
        return toDto(target, user);
    }

    /**
     * Locks the organization row ({@code PESSIMISTIC_WRITE}) and verifies more than one
     * ACTIVE OWNER remains, otherwise 409 {@code last-owner}.
     */
    private void ensureNotLastOwner(UUID organizationId) {
        organizations.lockForOwnerMutation(organizationId)
                .orElseThrow(OrganizationNotFoundException::new);
        long activeOwners = memberships.countByOrganizationIdAndRoleAndStatus(
                organizationId, OrganizationRole.OWNER, MembershipStatus.ACTIVE);
        if (activeOwners <= 1) {
            throw new LastOwnerException();
        }
    }

    private OrganizationMembership activeMembership(UUID organizationId, UUID membershipId) {
        return memberships.findByIdAndOrganizationId(membershipId, organizationId)
                .filter(OrganizationMembership::isActive)
                .orElseThrow(OrganizationNotFoundException::new);
    }

    private static MemberDto toDto(OrganizationMembership membership, UserSummaryDto user) {
        if (user == null) {
            throw new IllegalStateException(
                    "Membership " + membership.getId() + " references a missing user account");
        }
        return new MemberDto(
                membership.getId(),
                membership.getUserId(),
                user.displayName(),
                user.email(),
                membership.getRole(),
                membership.getJoinedAt());
    }
}
