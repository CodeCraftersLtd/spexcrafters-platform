package com.spexcrafters.taxonomy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Per-locale {@code name}/{@code description} of an {@link Attribute}. */
@Entity
@Table(name = "attribute_translation", schema = "taxonomy")
public class AttributeTranslation extends TaxonomyTranslation {

    @Column(name = "attribute_id", nullable = false)
    private UUID attributeId;

    @Column(name = "name", length = 300)
    private String name;

    @Column(name = "description", length = 4000)
    private String description;

    protected AttributeTranslation() {
        // JPA
    }

    public AttributeTranslation(UUID id, UUID attributeId, String locale, String sourceLocale, int sourceVersion,
            TranslationSource source, boolean original, UUID translatorUserId) {
        this.attributeId = attributeId;
        initLifecycle(id, locale, sourceLocale, sourceVersion, source, original, translatorUserId);
    }

    public void applyContent(String name, String description, int sourceVersion, TranslationSource source,
            UUID editorUserId) {
        this.name = name;
        this.description = description;
        applyLifecycleOnEdit(sourceVersion, source, editorUserId);
    }

    public UUID getAttributeId() {
        return attributeId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
