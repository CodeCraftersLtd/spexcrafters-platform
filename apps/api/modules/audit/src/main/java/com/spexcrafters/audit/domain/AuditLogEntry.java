package com.spexcrafters.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

/**
 * Append-only audit record. No audit-stamp columns or optimistic locking on purpose:
 * rows are written once and never updated (domain-model §I.1).
 *
 * <p>Implements {@link Persistable} so Spring Data persists pre-assigned UUIDv7 ids
 * directly instead of issuing a merge-select.
 */
@Entity
@Table(name = "audit_log", schema = "audit")
public class AuditLogEntry implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "target_type", length = 100)
    private String targetType;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    /**
     * Structured event payload as a raw JSON document (TD-9), e.g. the checked capability
     * of an authorization denial. Mapped as a plain string carrying pre-serialized JSON:
     * Hibernate writes it straight into the {@code jsonb} column without a format-mapper
     * round trip, and {@code ddl-auto=validate} accepts the mapping.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail")
    private String detail;

    @Column(name = "at", nullable = false)
    private Instant at;

    @Transient
    private boolean isNew = true;

    protected AuditLogEntry() {
        // JPA
    }

    public AuditLogEntry(UUID id, UUID actorUserId, String action, String targetType,
            String targetId, String correlationId, String detail, Instant at) {
        this.id = id;
        this.actorUserId = actorUserId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.correlationId = correlationId;
        this.detail = detail;
        this.at = at;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getAt() {
        return at;
    }
}
