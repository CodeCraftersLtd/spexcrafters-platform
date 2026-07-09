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
 * A supplier's verification case (one per supplier). Its {@link VerificationStatus} is an
 * informational rollup; the authoritative facts are the per-scope {@link VerificationScopeResult}
 * rows. Scope grants/suspensions/revocations are platform-staff-only actions.
 */
@Entity
@Table(name = "verification_case", schema = "verification")
public class VerificationCase extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "supplier_id", nullable = false, unique = true)
    private UUID supplierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private VerificationStatus status;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    protected VerificationCase() {
        // JPA
    }

    public VerificationCase(UUID id, UUID supplierId, Instant openedAt) {
        this.id = id;
        this.supplierId = supplierId;
        this.openedAt = openedAt;
        this.status = VerificationStatus.UNDER_REVIEW;
    }

    public void markStatus(VerificationStatus status) {
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }
}
