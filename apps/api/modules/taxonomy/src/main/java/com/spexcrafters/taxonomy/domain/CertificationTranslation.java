package com.spexcrafters.taxonomy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Per-locale {@code name}/{@code description} of a {@link Certification}. */
@Entity
@Table(name = "certification_translation", schema = "taxonomy")
public class CertificationTranslation extends TaxonomyTranslation {

    @Column(name = "certification_id", nullable = false)
    private UUID certificationId;

    @Column(name = "name", length = 300)
    private String name;

    @Column(name = "description", length = 4000)
    private String description;

    protected CertificationTranslation() {
        // JPA
    }

    public CertificationTranslation(UUID id, UUID certificationId, String locale, String sourceLocale,
            int sourceVersion, TranslationSource source, boolean original, UUID translatorUserId) {
        this.certificationId = certificationId;
        initLifecycle(id, locale, sourceLocale, sourceVersion, source, original, translatorUserId);
    }

    public void applyContent(String name, String description, int sourceVersion, TranslationSource source,
            UUID editorUserId) {
        this.name = name;
        this.description = description;
        applyLifecycleOnEdit(sourceVersion, source, editorUserId);
    }

    public UUID getCertificationId() {
        return certificationId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
