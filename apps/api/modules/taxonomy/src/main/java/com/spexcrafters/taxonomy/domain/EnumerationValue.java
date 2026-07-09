package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** A value within an {@link Enumeration} (domain-model §3), identified by its {@code code}. */
@Entity
@Table(name = "enumeration_value", schema = "taxonomy")
public class EnumerationValue extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "enumeration_id", nullable = false)
    private UUID enumerationId;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "deprecated", nullable = false)
    private boolean deprecated;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected EnumerationValue() {
        // JPA
    }

    public EnumerationValue(UUID id, UUID enumerationId, String code, int sortOrder) {
        this.id = id;
        this.enumerationId = enumerationId;
        this.code = code;
        this.sortOrder = sortOrder;
        this.deprecated = false;
        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEnumerationId() {
        return enumerationId;
    }

    public String getCode() {
        return code;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public boolean isActive() {
        return active;
    }
}
