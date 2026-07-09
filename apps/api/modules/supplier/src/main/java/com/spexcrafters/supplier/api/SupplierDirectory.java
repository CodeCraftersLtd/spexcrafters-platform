package com.spexcrafters.supplier.api;

import com.spexcrafters.supplier.domain.Supplier;
import com.spexcrafters.supplier.infrastructure.SupplierRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only supplier lookups for the verification context, which references suppliers when
 * opening verification cases and granting scopes. Part of the supplier module's public
 * {@code api} surface so verification never touches the supplier domain or tables.
 */
@Service
public class SupplierDirectory {

    private final SupplierRepository suppliers;

    public SupplierDirectory(SupplierRepository suppliers) {
        this.suppliers = suppliers;
    }

    @Transactional(readOnly = true)
    public Optional<SupplierRef> findSupplier(UUID supplierId) {
        return suppliers.findById(supplierId).map(SupplierDirectory::toRef);
    }

    private static SupplierRef toRef(Supplier supplier) {
        return new SupplierRef(supplier.getId(), supplier.getOrganizationId(), supplier.getOperationalStatus());
    }
}
