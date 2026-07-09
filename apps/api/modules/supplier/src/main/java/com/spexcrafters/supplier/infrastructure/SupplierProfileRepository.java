package com.spexcrafters.supplier.infrastructure;

import com.spexcrafters.supplier.domain.SupplierProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierProfileRepository extends JpaRepository<SupplierProfile, UUID> {

    Optional<SupplierProfile> findBySupplierId(UUID supplierId);
}
