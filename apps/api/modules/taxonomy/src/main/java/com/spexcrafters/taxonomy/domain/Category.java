package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A node in the unlimited-depth category tree (domain-model §2). Adjacency via {@code parentId}
 * plus a materialized {@code path} of {@code /}-joined ancestor codes (incl. self) for subtree
 * reads. {@code sourceVersion} bumps when the translatable source content changes.
 */
@Entity
@Table(name = "category", schema = "taxonomy")
public class Category extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "depth", nullable = false)
    private int depth;

    @Column(name = "path", nullable = false, length = 1000)
    private String path;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", nullable = false, length = 32)
    private CategoryClassification classification;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "source_version", nullable = false)
    private int sourceVersion;

    protected Category() {
        // JPA
    }

    public Category(UUID id, UUID parentId, String code, int depth, String path,
            CategoryClassification classification, int sortOrder) {
        this.id = id;
        this.parentId = parentId;
        this.code = code;
        this.depth = depth;
        this.path = path;
        this.classification = classification;
        this.active = true;
        this.sortOrder = sortOrder;
        this.sourceVersion = 1;
    }

    /** Bumps and returns the new source version (called when original-locale content changes). */
    public int bumpSourceVersion() {
        return ++sourceVersion;
    }

    public void reparent(UUID parentId, int depth, String path) {
        this.parentId = parentId;
        this.depth = depth;
        this.path = path;
    }

    public void setClassification(CategoryClassification classification) {
        this.classification = classification;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public UUID getParentId() {
        return parentId;
    }

    public String getCode() {
        return code;
    }

    public int getDepth() {
        return depth;
    }

    public String getPath() {
        return path;
    }

    public CategoryClassification getClassification() {
        return classification;
    }

    public boolean isActive() {
        return active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public int getSourceVersion() {
        return sourceVersion;
    }
}
