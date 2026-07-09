package com.spexcrafters.verification.infrastructure;

import com.spexcrafters.verification.domain.VerificationScopeResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationScopeResultRepository extends JpaRepository<VerificationScopeResult, UUID> {

    List<VerificationScopeResult> findByCaseIdOrderByScopeCodeAsc(UUID caseId);

    Optional<VerificationScopeResult> findByCaseIdAndScopeCode(UUID caseId, String scopeCode);
}
