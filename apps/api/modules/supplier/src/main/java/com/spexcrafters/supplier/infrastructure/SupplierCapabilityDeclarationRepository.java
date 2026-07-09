package com.spexcrafters.supplier.infrastructure;

import com.spexcrafters.supplier.domain.SupplierCapabilityDeclaration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierCapabilityDeclarationRepository
        extends JpaRepository<SupplierCapabilityDeclaration, UUID> {

    List<SupplierCapabilityDeclaration> findBySupplierIdOrderByCapabilityCodeAsc(UUID supplierId);

    Optional<SupplierCapabilityDeclaration> findBySupplierIdAndCapabilityCode(
            UUID supplierId, String capabilityCode);

    void deleteBySupplierId(UUID supplierId);
}
