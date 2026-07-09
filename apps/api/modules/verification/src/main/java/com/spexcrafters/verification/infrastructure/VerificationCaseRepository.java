package com.spexcrafters.verification.infrastructure;

import com.spexcrafters.verification.domain.VerificationCase;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationCaseRepository extends JpaRepository<VerificationCase, UUID> {

    Optional<VerificationCase> findBySupplierId(UUID supplierId);
}
