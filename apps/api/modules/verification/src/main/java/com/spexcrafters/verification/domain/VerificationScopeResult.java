package com.spexcrafters.verification.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * The verification result for one scope code within a case (supplier-domain-model §3). Unique
 * per {@code (case_id, scope_code)}. A scope reaches {@code VERIFIED} only through an explicit
 * platform-staff grant backed by evidence linkage; suspend/revoke are also staff-only and
 * append audited transitions (history is never deleted). {@code validUntil} bounds validity.
 */
@Entity
@Table(name = "verification_scope_result", schema = "verification")
public class VerificationScopeResult extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "scope_code", nullable = false, length = 64)
    private String scopeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private VerificationStatus status;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "reason", length = 4000)
    private String reason;

    protected VerificationScopeResult() {
        // JPA
    }

    public VerificationScopeResult(UUID id, UUID caseId, String scopeCode) {
        this.id = id;
        this.caseId = caseId;
        this.scopeCode = scopeCode;
        this.status = VerificationStatus.PENDING;
    }

    public void grant(UUID decidedBy, Instant now, Instant validUntil, String reason) {
        this.status = VerificationStatus.VERIFIED;
        this.decidedBy = decidedBy;
        this.decidedAt = now;
        this.validFrom = now;
        this.validUntil = validUntil;
        this.reason = reason;
    }

    public void suspend(UUID decidedBy, Instant now, String reason) {
        this.status = VerificationStatus.SUSPENDED;
        this.decidedBy = decidedBy;
        this.decidedAt = now;
        this.reason = reason;
    }

    public void revoke(UUID decidedBy, Instant now, String reason) {
        this.status = VerificationStatus.REVOKED;
        this.decidedBy = decidedBy;
        this.decidedAt = now;
        this.reason = reason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getScopeCode() {
        return scopeCode;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public String getReason() {
        return reason;
    }
}
