package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Import/synonym alias for an {@link EnumerationValue} (domain-model §3). */
@Entity
@Table(name = "enumeration_value_alias", schema = "taxonomy")
public class EnumerationValueAlias extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "enumeration_value_id", nullable = false)
    private UUID enumerationValueId;

    @Column(name = "alias", nullable = false, length = 120)
    private String alias;

    protected EnumerationValueAlias() {
        // JPA
    }

    public EnumerationValueAlias(UUID id, UUID enumerationValueId, String alias) {
        this.id = id;
        this.enumerationValueId = enumerationValueId;
        this.alias = alias;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEnumerationValueId() {
        return enumerationValueId;
    }

    public String getAlias() {
        return alias;
    }
}
