package com.spexcrafters.taxonomy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Per-locale {@code label} of an {@link AttributeAllowedValue}. */
@Entity
@Table(name = "attribute_allowed_value_translation", schema = "taxonomy")
public class AttributeAllowedValueTranslation extends TaxonomyTranslation {

    @Column(name = "attribute_allowed_value_id", nullable = false)
    private UUID attributeAllowedValueId;

    @Column(name = "label", length = 300)
    private String label;

    protected AttributeAllowedValueTranslation() {
        // JPA
    }

    public AttributeAllowedValueTranslation(UUID id, UUID attributeAllowedValueId, String locale,
            String sourceLocale, int sourceVersion, TranslationSource source, boolean original,
            UUID translatorUserId) {
        this.attributeAllowedValueId = attributeAllowedValueId;
        initLifecycle(id, locale, sourceLocale, sourceVersion, source, original, translatorUserId);
    }

    public void applyContent(String label, int sourceVersion, TranslationSource source, UUID editorUserId) {
        this.label = label;
        applyLifecycleOnEdit(sourceVersion, source, editorUserId);
    }

    public UUID getAttributeAllowedValueId() {
        return attributeAllowedValueId;
    }

    public String getLabel() {
        return label;
    }
}
