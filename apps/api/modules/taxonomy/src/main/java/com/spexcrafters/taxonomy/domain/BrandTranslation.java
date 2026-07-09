package com.spexcrafters.taxonomy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Per-locale {@code display_name}/{@code description} of a {@link Brand}. */
@Entity
@Table(name = "brand_translation", schema = "taxonomy")
public class BrandTranslation extends TaxonomyTranslation {

    @Column(name = "brand_id", nullable = false)
    private UUID brandId;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "description", length = 2000)
    private String description;

    protected BrandTranslation() {
        // JPA
    }

    public BrandTranslation(UUID id, UUID brandId, String locale, String sourceLocale, int sourceVersion,
            TranslationSource source, boolean original, UUID translatorUserId) {
        this.brandId = brandId;
        initLifecycle(id, locale, sourceLocale, sourceVersion, source, original, translatorUserId);
    }

    public void applyContent(String displayName, String description, int sourceVersion, TranslationSource source,
            UUID editorUserId) {
        this.displayName = displayName;
        this.description = description;
        applyLifecycleOnEdit(sourceVersion, source, editorUserId);
    }

    public UUID getBrandId() {
        return brandId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
