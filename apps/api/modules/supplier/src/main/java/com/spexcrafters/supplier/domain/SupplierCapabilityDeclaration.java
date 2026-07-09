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
 * A supplier's declared capability (class C code) with its {@link ClaimStatus}. A declaration
 * starts {@code CLAIMED} and never advances to {@code VERIFIED} without an explicit reviewer
 * verification action — the claim is not evidence of verification. Unique per
 * {@code (supplier_id, capability_code)}.
 */
@Entity
@Table(name = "supplier_capability_declaration", schema = "supplier")
public class SupplierCapabilityDeclaration extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "capability_code", nullable = false, length = 64)
    private String capabilityCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_status", nullable = false, length = 32)
    private ClaimStatus claimStatus;

    protected SupplierCapabilityDeclaration() {
        // JPA
    }

    public SupplierCapabilityDeclaration(UUID id, UUID supplierId, String capabilityCode) {
        this.id = id;
        this.supplierId = supplierId;
        this.capabilityCode = capabilityCode;
        this.claimStatus = ClaimStatus.CLAIMED;
    }

    /** Set by the verification context when a scope covering this capability is granted. */
    public void markVerified() {
        this.claimStatus = ClaimStatus.VERIFIED;
    }

    public void markEvidenceSubmitted() {
        if (claimStatus == ClaimStatus.CLAIMED) {
            this.claimStatus = ClaimStatus.EVIDENCE_SUBMITTED;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public String getCapabilityCode() {
        return capabilityCode;
    }

    public ClaimStatus getClaimStatus() {
        return claimStatus;
    }
}
