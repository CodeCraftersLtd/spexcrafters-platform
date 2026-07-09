package com.spexcrafters.taxonomy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Per-locale {@code name}/{@code description} of a {@link Category}. */
@Entity
@Table(name = "category_translation", schema = "taxonomy")
public class CategoryTranslation extends TaxonomyTranslation {

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "name", length = 300)
    private String name;

    @Column(name = "description", length = 4000)
    private String description;

    protected CategoryTranslation() {
        // JPA
    }

    public CategoryTranslation(UUID id, UUID categoryId, String locale, String sourceLocale, int sourceVersion,
            TranslationSource source, boolean original, UUID translatorUserId) {
        this.categoryId = categoryId;
        initLifecycle(id, locale, sourceLocale, sourceVersion, source, original, translatorUserId);
    }

    public void applyContent(String name, String description, int sourceVersion, TranslationSource source,
            UUID editorUserId) {
        this.name = name;
        this.description = description;
        applyLifecycleOnEdit(sourceVersion, source, editorUserId);
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
