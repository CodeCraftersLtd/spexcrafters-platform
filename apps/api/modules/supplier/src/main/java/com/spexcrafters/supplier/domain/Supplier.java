package com.spexcrafters.supplier.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Supplier aggregate root (supplier-domain-model §1, §3). Belongs to exactly one
 * organization; a partial unique index enforces at most one non-terminal supplier per
 * organization. Its {@link OperationalStatus} is separate from the application lifecycle:
 * approving the application flips this to {@code ACTIVE} in the same transaction.
 */
@Entity
@Table(name = "supplier", schema = "supplier")
public class Supplier extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 32)
    private OperationalStatus operationalStatus;

    @Column(name = "original_locale", nullable = false, length = 16)
    private String originalLocale;

    protected Supplier() {
        // JPA
    }

    public Supplier(UUID id, UUID organizationId, String originalLocale) {
        if (id == null || organizationId == null) {
            throw new IllegalArgumentException("id and organizationId are required");
        }
        if (originalLocale == null || originalLocale.isBlank()) {
            throw new IllegalArgumentException("originalLocale is required");
        }
        this.id = id;
        this.organizationId = organizationId;
        this.originalLocale = originalLocale;
        this.operationalStatus = OperationalStatus.PENDING;
    }

    /** Approval activates the supplier (supplier-domain-model §4). Idempotent from ACTIVE. */
    public void activate() {
        if (operationalStatus == OperationalStatus.DEACTIVATED) {
            throw new IllegalStateException("A deactivated supplier cannot be reactivated");
        }
        this.operationalStatus = OperationalStatus.ACTIVE;
    }

    /** Platform suspension of the supplier (distinct from verification-scope suspension). */
    public void suspend() {
        this.operationalStatus = OperationalStatus.SUSPENDED;
    }

    /**
     * Marks the supplier identity gone (application withdrawn or rejected), freeing the
     * organization's single-active-supplier slot so it may apply again. The row and its audit
     * history are retained.
     */
    public void deactivate() {
        this.operationalStatus = OperationalStatus.DEACTIVATED;
    }

    public boolean isActive() {
        return operationalStatus == OperationalStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public OperationalStatus getOperationalStatus() {
        return operationalStatus;
    }

    public String getOriginalLocale() {
        return originalLocale;
    }
}
