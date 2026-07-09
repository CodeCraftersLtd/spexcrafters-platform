package com.spexcrafters.supplier.infrastructure;

import com.spexcrafters.supplier.domain.SupplierFacility;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierFacilityRepository extends JpaRepository<SupplierFacility, UUID> {

    List<SupplierFacility> findBySupplierIdOrderByCreatedAtAsc(UUID supplierId);

    Optional<SupplierFacility> findByIdAndSupplierId(UUID id, UUID supplierId);
}
