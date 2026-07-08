package com.spexcrafters.sharedkernel.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * AuditStamp base entity: the standard audit columns required on every business table
 * (domain-model §I.1) plus optimistic locking.
 *
 * <p>{@code createdAt}/{@code updatedAt} are maintained by JPA lifecycle callbacks.
 * {@code createdBy}/{@code updatedBy} are nullable (system-initiated actions) and set
 * explicitly by application services when an authenticated actor is known.
 *
 * <p>{@code version} is the wrapper type {@link Integer} on purpose: Spring Data uses a
 * {@code null} version to detect new aggregates, so {@code save(...)} persists directly
 * instead of issuing a merge-select for our pre-assigned UUIDv7 ids.
 */
@MappedSuperclass
public abstract class AuditedEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    void onPrePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onPreUpdate() {
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Integer getVersion() {
        return version;
    }
}
