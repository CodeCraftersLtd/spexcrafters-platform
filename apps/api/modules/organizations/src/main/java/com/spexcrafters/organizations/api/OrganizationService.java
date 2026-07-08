package com.spexcrafters.organizations.api;

import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.organizations.domain.Capability;
import com.spexcrafters.organizations.domain.Organization;
import com.spexcrafters.organizations.domain.OrganizationMembership;
import com.spexcrafters.organizations.domain.OrganizationRole;
import com.spexcrafters.organizations.domain.OrganizationType;
import com.spexcrafters.organizations.infrastructure.OrganizationMembershipRepository;
import com.spexcrafters.organizations.infrastructure.OrganizationRepository;
import com.spexcrafters.sharedkernel.problem.ConflictException;
import com.spexcrafters.sharedkernel.util.UuidV7;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Organization lifecycle: creation (with the atomic initial OWNER membership) and profile. */
@Service
public class OrganizationService {

    private static final String STALE_VERSION_DETAIL =
            "The organization was modified by someone else. Reload it and retry with the current version.";

    private final OrganizationRepository organizations;
    private final OrganizationMembershipRepository memberships;
    private final OrganizationAccess access;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public OrganizationService(OrganizationRepository organizations,
            OrganizationMembershipRepository memberships,
            OrganizationAccess access,
            AuditLogger auditLogger,
            Clock clock) {
        this.organizations = organizations;
        this.memberships = memberships;
        this.access = access;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    /**
     * Persists the organization and the creator's OWNER membership in one transaction
     * (organizations-capability-model.md §3: atomic initial owner).
     */
    @Transactional
    public OrganizationDto create(UUID creatorUserId, String name, OrganizationType type, String country) {
        Organization organization = new Organization(UuidV7.generate(), name, type, normalizeCountry(country));
        organization.setCreatedBy(creatorUserId);
        organization.setUpdatedBy(creatorUserId);
        organizations.save(organization);

        OrganizationMembership ownerMembership = new OrganizationMembership(
                UuidV7.generate(), organization.getId(), creatorUserId, OrganizationRole.OWNER, clock.instant());
        ownerMembership.setCreatedBy(creatorUserId);
        ownerMembership.setUpdatedBy(creatorUserId);
        memberships.save(ownerMembership);

        auditLogger.record("organization.created", creatorUserId,
                "organization", organization.getId().toString());
        return toDto(organization, ownerMembership.getRole());
    }

    @Transactional(readOnly = true)
    public OrganizationDto get(UUID userId, UUID organizationId) {
        OrganizationContext context = access.require(userId, organizationId, Capability.ORGANIZATION_READ);
        return toDto(context.organization(), context.role());
    }

    /**
     * Partial update ({@code null} field = leave unchanged) guarded by the optimistic
     * {@code version} from the last read: a stale version — whether detected up front or
     * by Hibernate's version check at flush — is a 409 conflict.
     */
    @Transactional
    public OrganizationDto update(UUID userId, UUID organizationId, String name, String country,
            int expectedVersion) {
        OrganizationContext context = access.require(userId, organizationId, Capability.ORGANIZATION_UPDATE);
        Organization organization = context.organization();
        if (organization.getVersion() != expectedVersion) {
            throw new ConflictException(STALE_VERSION_DETAIL);
        }
        if (name != null) {
            organization.rename(name);
        }
        if (country != null) {
            organization.assignCountry(normalizeCountry(country));
        }
        organization.setUpdatedBy(userId);
        try {
            organizations.flush();
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflictException(STALE_VERSION_DETAIL);
        }
        auditLogger.record("organization.updated", userId, "organization", organizationId.toString());
        return toDto(organization, context.role());
    }

    private static String normalizeCountry(String country) {
        return country == null ? null : country.trim().toUpperCase(Locale.ROOT);
    }

    static OrganizationDto toDto(Organization organization, OrganizationRole callerRole) {
        return new OrganizationDto(
                organization.getId(),
                organization.getName(),
                organization.getType(),
                organization.getCountry(),
                organization.getCreatedAt(),
                organization.getVersion(),
                callerRole,
                sorted(Capability.forRole(callerRole)));
    }

    private static List<Capability> sorted(Set<Capability> capabilities) {
        return capabilities.stream().sorted(Comparator.naturalOrder()).toList();
    }
}
