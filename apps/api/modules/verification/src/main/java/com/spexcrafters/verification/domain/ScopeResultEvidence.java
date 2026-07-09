package com.spexcrafters.verification.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Links a verification scope result to the evidence that supports it (the "evidence linkage"
 * invariant: a grant requires evidence). {@code evidenceId} references supplier-owned evidence;
 * the FK is a database integrity net only (cross-module table access is forbidden in code).
 */
@Entity
@Table(name = "scope_result_evidence", schema = "verification")
public class ScopeResultEvidence extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "scope_result_id", nullable = false)
    private UUID scopeResultId;

    @Column(name = "evidence_id", nullable = false)
    private UUID evidenceId;

    protected ScopeResultEvidence() {
        // JPA
    }

    public ScopeResultEvidence(UUID id, UUID scopeResultId, UUID evidenceId) {
        this.id = id;
        this.scopeResultId = scopeResultId;
        this.evidenceId = evidenceId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getScopeResultId() {
        return scopeResultId;
    }

    public UUID getEvidenceId() {
        return evidenceId;
    }
}
