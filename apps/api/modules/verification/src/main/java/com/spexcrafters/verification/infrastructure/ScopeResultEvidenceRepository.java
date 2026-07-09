package com.spexcrafters.verification.infrastructure;

import com.spexcrafters.verification.domain.ScopeResultEvidence;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScopeResultEvidenceRepository extends JpaRepository<ScopeResultEvidence, UUID> {

    List<ScopeResultEvidence> findByScopeResultId(UUID scopeResultId);

    void deleteByScopeResultId(UUID scopeResultId);
}
