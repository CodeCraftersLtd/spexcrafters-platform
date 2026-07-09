package com.spexcrafters.supplier.infrastructure;

import com.spexcrafters.supplier.domain.ApplicationStatus;
import com.spexcrafters.supplier.domain.SupplierApplication;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierApplicationRepository extends JpaRepository<SupplierApplication, UUID> {

    Optional<SupplierApplication> findBySupplierId(UUID supplierId);

    /** First page of the reviewer queue (cursor = id ascending; UUIDv7 is time-ordered). */
    List<SupplierApplication> findByStatusInOrderByIdAsc(Collection<ApplicationStatus> statuses, Limit limit);

    /** Subsequent pages: everything after the opaque {@code id} cursor. */
    List<SupplierApplication> findByStatusInAndIdGreaterThanOrderByIdAsc(
            Collection<ApplicationStatus> statuses, UUID afterId, Limit limit);
}
