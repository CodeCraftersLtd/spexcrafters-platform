package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A localized SEO slug for a {@link Category} (ADR-027). Unique per {@code (locale, slug)}. One
 * primary per {@code (category, locale)} is enforced in the service; non-primary/inactive rows
 * are historical aliases retained for 301 redirects.
 */
@Entity
@Table(name = "category_slug", schema = "taxonomy")
public class CategorySlug extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "locale", nullable = false, length = 16)
    private String locale;

    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected CategorySlug() {
        // JPA
    }

    public CategorySlug(UUID id, UUID categoryId, String locale, String slug, boolean primary) {
        this.id = id;
        this.categoryId = categoryId;
        this.locale = locale;
        this.slug = slug;
        this.primary = primary;
        this.active = true;
    }

    public void demote() {
        this.primary = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getLocale() {
        return locale;
    }

    public String getSlug() {
        return slug;
    }

    public boolean isPrimary() {
        return primary;
    }

    public boolean isActive() {
        return active;
    }
}
