package com.spexcrafters.taxonomy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Per-locale {@code label}/{@code description} of an {@link EnumerationValue}. */
@Entity
@Table(name = "enumeration_value_translation", schema = "taxonomy")
public class EnumerationValueTranslation extends TaxonomyTranslation {

    @Column(name = "enumeration_value_id", nullable = false)
    private UUID enumerationValueId;

    @Column(name = "label", length = 300)
    private String label;

    @Column(name = "description", length = 2000)
    private String description;

    protected EnumerationValueTranslation() {
        // JPA
    }

    public EnumerationValueTranslation(UUID id, UUID enumerationValueId, String locale, String sourceLocale,
            int sourceVersion, TranslationSource source, boolean original, UUID translatorUserId) {
        this.enumerationValueId = enumerationValueId;
        initLifecycle(id, locale, sourceLocale, sourceVersion, source, original, translatorUserId);
    }

    public void applyContent(String label, String description, int sourceVersion, TranslationSource source,
            UUID editorUserId) {
        this.label = label;
        this.description = description;
        applyLifecycleOnEdit(sourceVersion, source, editorUserId);
    }

    public UUID getEnumerationValueId() {
        return enumerationValueId;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
