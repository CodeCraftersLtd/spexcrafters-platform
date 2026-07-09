package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** A reusable controlled vocabulary (domain-model §3). Stable id {@code code}; PK UUIDv7. */
@Entity
@Table(name = "enumeration", schema = "taxonomy")
public class Enumeration extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "description_key", length = 120)
    private String descriptionKey;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected Enumeration() {
        // JPA
    }

    public Enumeration(UUID id, String code, String descriptionKey) {
        this.id = id;
        this.code = code;
        this.descriptionKey = descriptionKey;
        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public boolean isActive() {
        return active;
    }
}
