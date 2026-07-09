package com.spexcrafters.supplier.domain;

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
 * The onboarding application for a {@link Supplier}. Owns the lifecycle state machine
 * (supplier-domain-model §4). Transitions are validated against
 * {@link ApplicationStatus#allowedTransitions()}; an illegal transition throws
 * {@link InvalidApplicationTransitionException} (409). The inherited optimistic {@code version}
 * guards concurrent edits.
 */
@Entity
@Table(name = "supplier_application", schema = "supplier")
public class SupplierApplication extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ApplicationStatus status;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "claimed_by")
    private UUID claimedBy;

    protected SupplierApplication() {
        // JPA
    }

    public SupplierApplication(UUID id, UUID supplierId) {
        if (id == null || supplierId == null) {
            throw new IllegalArgumentException("id and supplierId are required");
        }
        this.id = id;
        this.supplierId = supplierId;
        this.status = ApplicationStatus.DRAFT;
    }

    /** Supplier submits the draft for review. */
    public void submit(Instant now) {
        transitionTo(ApplicationStatus.SUBMITTED);
        this.submittedAt = now;
    }

    /** Reviewer claims a SUBMITTED/RESUBMITTED application, moving it UNDER_REVIEW. */
    public void claimForReview(UUID reviewerUserId) {
        transitionTo(ApplicationStatus.UNDER_REVIEW);
        this.claimedBy = reviewerUserId;
    }

    /** Reviewer requests changes; the supplier can then edit and resubmit. */
    public void requestChanges() {
        transitionTo(ApplicationStatus.CHANGES_REQUESTED);
    }

    /** Supplier resubmits after addressing change requests. */
    public void resubmit(Instant now) {
        transitionTo(ApplicationStatus.RESUBMITTED);
        this.submittedAt = now;
    }

    /** Reviewer approves; caller must activate the supplier in the same transaction. */
    public void approve(UUID reviewerUserId, Instant now) {
        transitionTo(ApplicationStatus.APPROVED);
        this.decidedBy = reviewerUserId;
        this.decidedAt = now;
    }

    /** Reviewer rejects the application (terminal; reapplication is a new application). */
    public void reject(UUID reviewerUserId, Instant now) {
        transitionTo(ApplicationStatus.REJECTED);
        this.decidedBy = reviewerUserId;
        this.decidedAt = now;
    }

    /** Supplier withdraws from any pre-decision state. */
    public void withdraw() {
        transitionTo(ApplicationStatus.WITHDRAWN);
    }

    private void transitionTo(ApplicationStatus target) {
        if (!status.allowedTransitions().contains(target)) {
            throw new InvalidApplicationTransitionException(status, target);
        }
        this.status = target;
    }

    public boolean isEditableBySupplier() {
        return status.isEditableBySupplier();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public UUID getClaimedBy() {
        return claimedBy;
    }
}
