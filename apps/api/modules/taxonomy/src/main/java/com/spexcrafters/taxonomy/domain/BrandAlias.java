package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Synonym/legacy-name alias for a {@link Brand} (domain-model §8). */
@Entity
@Table(name = "brand_alias", schema = "taxonomy")
public class BrandAlias extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "brand_id", nullable = false)
    private UUID brandId;

    @Column(name = "alias", nullable = false, length = 200)
    private String alias;

    protected BrandAlias() {
        // JPA
    }

    public BrandAlias(UUID id, UUID brandId, String alias) {
        this.id = id;
        this.brandId = brandId;
        this.alias = alias;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBrandId() {
        return brandId;
    }

    public String getAlias() {
        return alias;
    }
}
