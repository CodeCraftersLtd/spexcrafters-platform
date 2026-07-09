package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Inline controlled value for an attribute not backed by a shared enumeration (domain-model
 * §4; rare — prefer enumerations). No seed data ships for it.
 */
@Entity
@Table(name = "attribute_allowed_value", schema = "taxonomy")
public class AttributeAllowedValue extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "attribute_id", nullable = false)
    private UUID attributeId;

    @Column(name = "value_code", nullable = false, length = 64)
    private String valueCode;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected AttributeAllowedValue() {
        // JPA
    }

    public AttributeAllowedValue(UUID id, UUID attributeId, String valueCode, int sortOrder) {
        this.id = id;
        this.attributeId = attributeId;
        this.valueCode = valueCode;
        this.sortOrder = sortOrder;
        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAttributeId() {
        return attributeId;
    }

    public String getValueCode() {
        return valueCode;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }
}
