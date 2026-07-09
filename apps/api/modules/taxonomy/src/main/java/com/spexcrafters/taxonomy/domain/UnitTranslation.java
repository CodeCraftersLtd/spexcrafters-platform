package com.spexcrafters.taxonomy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Per-locale {@code display_name} of a {@link UnitOfMeasure} (keyed by {@code unit_code}). */
@Entity
@Table(name = "unit_translation", schema = "taxonomy")
public class UnitTranslation extends TaxonomyTranslation {

    @Column(name = "unit_code", nullable = false, length = 32)
    private String unitCode;

    @Column(name = "display_name", length = 120)
    private String displayName;

    protected UnitTranslation() {
        // JPA
    }

    public UnitTranslation(UUID id, String unitCode, String locale, String sourceLocale, int sourceVersion,
            TranslationSource source, boolean original, UUID translatorUserId) {
        this.unitCode = unitCode;
        initLifecycle(id, locale, sourceLocale, sourceVersion, source, original, translatorUserId);
    }

    public void applyContent(String displayName, int sourceVersion, TranslationSource source, UUID editorUserId) {
        this.displayName = displayName;
        applyLifecycleOnEdit(sourceVersion, source, editorUserId);
    }

    public String getUnitCode() {
        return unitCode;
    }

    public String getDisplayName() {
        return displayName;
    }
}
