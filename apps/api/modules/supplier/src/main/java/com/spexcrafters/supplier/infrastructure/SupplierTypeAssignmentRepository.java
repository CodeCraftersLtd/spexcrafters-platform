package com.spexcrafters.supplier.infrastructure;

import com.spexcrafters.supplier.domain.SupplierTypeAssignment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierTypeAssignmentRepository extends JpaRepository<SupplierTypeAssignment, UUID> {

    List<SupplierTypeAssignment> findBySupplierIdOrderByTypeCodeAsc(UUID supplierId);

    void deleteBySupplierId(UUID supplierId);
}
