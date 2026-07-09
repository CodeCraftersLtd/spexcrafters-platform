package com.spexcrafters.supplier.infrastructure;

import com.spexcrafters.supplier.domain.EvidenceUploadState;
import com.spexcrafters.supplier.domain.VerificationEvidence;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationEvidenceRepository extends JpaRepository<VerificationEvidence, UUID> {

    List<VerificationEvidence> findBySupplierIdAndUploadStateOrderByCreatedAtAsc(
            UUID supplierId, EvidenceUploadState uploadState);

    /** Tenant-scoped lookup; a non-matching supplier id yields empty (IDOR concealment). */
    Optional<VerificationEvidence> findByIdAndSupplierId(UUID id, UUID supplierId);
}
