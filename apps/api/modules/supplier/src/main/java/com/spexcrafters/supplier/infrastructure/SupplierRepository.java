package com.spexcrafters.supplier.infrastructure;

import com.spexcrafters.supplier.domain.OperationalStatus;
import com.spexcrafters.supplier.domain.Supplier;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    /**
     * The organization's occupying (non-deactivated) supplier, if any. Used for the friendly
     * pre-check of the one-active-supplier invariant; the partial unique index is the hard,
     * race-proof guard.
     */
    Optional<Supplier> findFirstByOrganizationIdAndOperationalStatusNot(
            UUID organizationId, OperationalStatus excluded);
}
