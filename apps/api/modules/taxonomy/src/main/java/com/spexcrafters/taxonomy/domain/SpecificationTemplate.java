package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A category's specification layout (domain-model §9). One template per category. The
 * <em>effective</em> template (own + inherited ancestor attributes) is resolved in the service.
 * {@code templateVersion} is the structural version (bumped on attribute add/remove) — distinct
 * from the audit-tail optimistic-lock {@code version} (see V5 migration deviation note).
 */
@Entity
@Table(name = "specification_template", schema = "taxonomy")
public class SpecificationTemplate extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "template_version", nullable = false)
    private int templateVersion;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected SpecificationTemplate() {
        // JPA
    }

    public SpecificationTemplate(UUID id, UUID categoryId, String code) {
        this.id = id;
        this.categoryId = categoryId;
        this.code = code;
        this.templateVersion = 1;
        this.active = true;
    }

    public int bumpTemplateVersion() {
        return ++templateVersion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getCode() {
        return code;
    }

    public int getTemplateVersion() {
        return templateVersion;
    }

    public boolean isActive() {
        return active;
    }
}
