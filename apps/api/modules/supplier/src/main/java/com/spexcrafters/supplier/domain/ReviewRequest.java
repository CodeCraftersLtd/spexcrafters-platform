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
 * A change request raised by a reviewer against an application (supplier-domain-model §3).
 * The supplier responds; the reviewer resolves it. Response content is class-D supplier text
 * carrying its own locale.
 */
@Entity
@Table(name = "review_request", schema = "supplier")
public class ReviewRequest extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "requested_item", nullable = false, length = 200)
    private String requestedItem;

    @Column(name = "reason", nullable = false, length = 4000)
    private String reason;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ReviewRequestStatus status;

    @Column(name = "supplier_response", length = 4000)
    private String supplierResponse;

    @Column(name = "response_locale", length = 16)
    private String responseLocale;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected ReviewRequest() {
        // JPA
    }

    public ReviewRequest(UUID id, UUID applicationId, String requestedItem, String reason,
            UUID requestedBy, Instant requestedAt) {
        this.id = id;
        this.applicationId = applicationId;
        this.requestedItem = requestedItem;
        this.reason = reason;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
        this.status = ReviewRequestStatus.OPEN;
    }

    public void respond(String response, String responseLocale) {
        this.supplierResponse = response;
        this.responseLocale = responseLocale;
        this.status = ReviewRequestStatus.RESPONDED;
    }

    public void resolve(Instant now) {
        this.status = ReviewRequestStatus.RESOLVED;
        this.resolvedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getRequestedItem() {
        return requestedItem;
    }

    public String getReason() {
        return reason;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public ReviewRequestStatus getStatus() {
        return status;
    }

    public String getSupplierResponse() {
        return supplierResponse;
    }

    public String getResponseLocale() {
        return responseLocale;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
