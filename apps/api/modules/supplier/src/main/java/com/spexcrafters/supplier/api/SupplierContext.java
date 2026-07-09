package com.spexcrafters.supplier.api;

import com.spexcrafters.organizations.api.OrgRole;
import com.spexcrafters.supplier.domain.Supplier;
import java.util.Set;
import java.util.UUID;

/**
 * The resolved authorization context of a supplier-scoped request: the supplier, the caller's
 * organization role and the supplier capabilities it confers. Produced exclusively by
 * {@link SupplierAccess}; consumed by application services (it carries a JPA entity, never a
 * controller).
 */
public record SupplierContext(
        Supplier supplier,
        UUID organizationId,
        OrgRole role,
        Set<SupplierCapability> capabilities) {

    public boolean has(SupplierCapability capability) {
        return capabilities.contains(capability);
    }

    public UUID supplierId() {
        return supplier.getId();
    }
}
