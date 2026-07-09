package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Global alias code resolving a retired/merged category code to its successor (§2). */
@Entity
@Table(name = "category_alias", schema = "taxonomy")
public class CategoryAlias extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "alias_code", nullable = false, length = 64)
    private String aliasCode;

    protected CategoryAlias() {
        // JPA
    }

    public CategoryAlias(UUID id, UUID categoryId, String aliasCode) {
        this.id = id;
        this.categoryId = categoryId;
        this.aliasCode = aliasCode;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getAliasCode() {
        return aliasCode;
    }
}
