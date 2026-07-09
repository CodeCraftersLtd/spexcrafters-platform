package com.spexcrafters.organizations.api;

import com.spexcrafters.organizations.domain.MembershipStatus;
import com.spexcrafters.organizations.domain.Organization;
import com.spexcrafters.organizations.domain.OrganizationMembership;
import com.spexcrafters.organizations.domain.OrganizationRole;
import com.spexcrafters.organizations.infrastructure.OrganizationMembershipRepository;
import com.spexcrafters.organizations.infrastructure.OrganizationRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only membership lookups for other modules (the supplier context resolves the caller's
 * organization role to derive supplier capabilities, mirroring how identity's
 * {@code UserDirectory} serves cross-module user lookups). Part of the organizations module's
 * public {@code api} surface, so cross-module access never touches its domain or tables.
 */
@Service
public class OrganizationDirectory {

    private final OrganizationRepository organizations;
    private final OrganizationMembershipRepository memberships;

    public OrganizationDirectory(OrganizationRepository organizations,
            OrganizationMembershipRepository memberships) {
        this.organizations = organizations;
        this.memberships = memberships;
    }

    /**
     * The caller's ACTIVE membership in the organization, or empty if the organization does
     * not exist or the caller has no ACTIVE membership. Consumers use "empty" for tenancy
     * 404-concealment.
     */
    @Transactional(readOnly = true)
    public Optional<OrgMembershipView> findActiveMembership(UUID organizationId, UUID userId) {
        Optional<Organization> organization = organizations.findById(organizationId);
        if (organization.isEmpty()) {
            return Optional.empty();
        }
        Optional<OrganizationMembership> membership =
                memberships.findByOrganizationIdAndUserIdAndStatus(organizationId, userId, MembershipStatus.ACTIVE);
        return membership.map(m -> new OrgMembershipView(
                organizationId, userId, toApiRole(m.getRole()), organization.get().isActive()));
    }

    /** Whether the organization exists (used to distinguish 404 from 403 concealment paths). */
    @Transactional(readOnly = true)
    public boolean organizationExists(UUID organizationId) {
        return organizations.findById(organizationId).isPresent();
    }

    private static OrgRole toApiRole(OrganizationRole role) {
        return switch (role) {
            case OWNER -> OrgRole.OWNER;
            case ADMIN -> OrgRole.ADMIN;
            case MEMBER -> OrgRole.MEMBER;
        };
    }
}
