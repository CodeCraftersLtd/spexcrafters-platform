package com.spexcrafters.organizations.api;

import com.spexcrafters.organizations.domain.AuthorizationDeniedException;
import com.spexcrafters.organizations.domain.Capability;
import com.spexcrafters.organizations.domain.MembershipStatus;
import com.spexcrafters.organizations.domain.Organization;
import com.spexcrafters.organizations.domain.OrganizationMembership;
import com.spexcrafters.organizations.domain.OrganizationNotFoundException;
import com.spexcrafters.organizations.infrastructure.OrganizationMembershipRepository;
import com.spexcrafters.organizations.infrastructure.OrganizationRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * The authorization policy of the organizations bounded context
 * (organizations-capability-model.md §5). Client-supplied organization ids are never
 * trusted: every org-scoped request resolves user → organization → ACTIVE membership →
 * role → capabilities here, in the application service layer — never in controllers.
 *
 * <p>Non-members and requests for unknown organizations receive an indistinguishable 404
 * ({@code not-found} problem) so organization existence cannot be probed; members lacking
 * a capability receive 403 ({@code authorization} problem). Both outcomes emit an
 * {@code authorization.denied} audit event that survives the transaction rollback.
 */
@Service
public class OrganizationAccess {

    private final OrganizationRepository organizations;
    private final OrganizationMembershipRepository memberships;
    private final AuthorizationDenialAuditor denialAuditor;

    public OrganizationAccess(OrganizationRepository organizations,
            OrganizationMembershipRepository memberships,
            AuthorizationDenialAuditor denialAuditor) {
        this.organizations = organizations;
        this.memberships = memberships;
        this.denialAuditor = denialAuditor;
    }

    /**
     * Resolves the caller's membership and capabilities without enforcing a specific
     * capability (used by flows whose authorization is contextual, e.g. self-removal).
     * Unknown organization or no ACTIVE membership → concealed 404, audited against
     * {@code contextCapability}.
     */
    public OrganizationContext resolve(UUID userId, UUID organizationId, Capability contextCapability) {
        Optional<Organization> organization = organizations.findById(organizationId);
        Optional<OrganizationMembership> membership =
                memberships.findByOrganizationIdAndUserIdAndStatus(organizationId, userId, MembershipStatus.ACTIVE);
        if (organization.isEmpty() || membership.isEmpty()) {
            denialAuditor.recordDenied(userId, organizationId, contextCapability.wireName());
            throw new OrganizationNotFoundException();
        }
        return new OrganizationContext(organization.get(), membership.get(),
                Capability.forRole(membership.get().getRole()));
    }

    /** Resolves the caller's context and requires {@code capability}, else audited 403. */
    public OrganizationContext require(UUID userId, UUID organizationId, Capability capability) {
        OrganizationContext context = resolve(userId, organizationId, capability);
        if (!context.has(capability)) {
            throw deny(userId, organizationId, capability.wireName());
        }
        return context;
    }

    /**
     * Records an {@code authorization.denied} audit event and returns (for throwing) the
     * 403 problem. Used by services for rank-rule violations that go beyond the plain
     * capability matrix (e.g. ADMIN touching an ADMIN membership).
     */
    public AuthorizationDeniedException deny(UUID userId, UUID organizationId, String checkedCapability) {
        denialAuditor.recordDenied(userId, organizationId, checkedCapability);
        return new AuthorizationDeniedException();
    }
}
