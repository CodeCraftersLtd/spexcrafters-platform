package com.spexcrafters.supplier.api;

import com.spexcrafters.organizations.api.OrgMembershipView;
import com.spexcrafters.organizations.api.OrganizationDirectory;
import com.spexcrafters.supplier.domain.Supplier;
import com.spexcrafters.supplier.domain.SupplierAuthorizationDeniedException;
import com.spexcrafters.supplier.domain.SupplierNotFoundException;
import com.spexcrafters.supplier.infrastructure.SupplierRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The authorization policy of the supplier bounded context (supplier-domain-model §5),
 * mirroring {@code OrganizationAccess}. Supplier capabilities are derived from the caller's
 * ACTIVE organization membership (resolved via the organizations {@code api}); the supplier
 * id or organization id from the request is never trusted directly.
 *
 * <p>Non-members and unknown suppliers/organizations receive an indistinguishable 404 so
 * existence and cross-tenant relationships cannot be probed (defeats IDOR); members lacking
 * a capability receive 403. Both emit an audited {@code authorization.denied} event.
 */
@Service
public class SupplierAccess {

    private final SupplierRepository suppliers;
    private final OrganizationDirectory organizationDirectory;
    private final SupplierAccessDenialAuditor denialAuditor;

    public SupplierAccess(SupplierRepository suppliers,
            OrganizationDirectory organizationDirectory,
            SupplierAccessDenialAuditor denialAuditor) {
        this.suppliers = suppliers;
        this.organizationDirectory = organizationDirectory;
        this.denialAuditor = denialAuditor;
    }

    /**
     * Requires {@code capability} for an organization-scoped action that has no supplier yet
     * (e.g. creating the application). Concealed 404 when the org is unknown or the caller has
     * no ACTIVE membership; audited 403 when the role lacks the capability.
     */
    @Transactional(readOnly = true)
    public OrgMembershipView requireForOrganization(UUID userId, UUID organizationId,
            SupplierCapability capability) {
        Optional<OrgMembershipView> membership =
                organizationDirectory.findActiveMembership(organizationId, userId);
        if (membership.isEmpty()) {
            denialAuditor.recordDenied(userId, "organization", organizationId,
                    capability.wireName(), organizationId);
            throw new SupplierNotFoundException();
        }
        OrgMembershipView view = membership.get();
        if (!SupplierCapability.forRole(view.role()).contains(capability)) {
            denialAuditor.recordDenied(userId, "organization", organizationId,
                    capability.wireName(), organizationId);
            throw new SupplierAuthorizationDeniedException();
        }
        return view;
    }

    /**
     * Requires {@code capability} for a supplier-scoped action. Concealed 404 when the
     * supplier is unknown or the caller is not an ACTIVE member of its owning organization;
     * audited 403 when the role lacks the capability.
     */
    @Transactional(readOnly = true)
    public SupplierContext requireForSupplier(UUID userId, UUID supplierId, SupplierCapability capability) {
        Optional<Supplier> supplier = suppliers.findById(supplierId);
        if (supplier.isEmpty()) {
            denialAuditor.recordDenied(userId, "supplier", supplierId, capability.wireName(), null);
            throw new SupplierNotFoundException();
        }
        UUID organizationId = supplier.get().getOrganizationId();
        Optional<OrgMembershipView> membership =
                organizationDirectory.findActiveMembership(organizationId, userId);
        if (membership.isEmpty()) {
            denialAuditor.recordDenied(userId, "supplier", supplierId, capability.wireName(), organizationId);
            throw new SupplierNotFoundException();
        }
        OrgMembershipView view = membership.get();
        var capabilities = SupplierCapability.forRole(view.role());
        if (!capabilities.contains(capability)) {
            denialAuditor.recordDenied(userId, "supplier", supplierId, capability.wireName(), organizationId);
            throw new SupplierAuthorizationDeniedException();
        }
        return new SupplierContext(supplier.get(), organizationId, view.role(), capabilities);
    }
}
