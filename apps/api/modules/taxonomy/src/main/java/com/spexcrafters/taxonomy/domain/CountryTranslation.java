package com.spexcrafters.taxonomy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Per-locale {@code name} of a {@link Country} (keyed by {@code country_code}). */
@Entity
@Table(name = "country_translation", schema = "taxonomy")
public class CountryTranslation extends TaxonomyTranslation {

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "name", length = 160)
    private String name;

    protected CountryTranslation() {
        // JPA
    }

    public CountryTranslation(UUID id, String countryCode, String locale, String sourceLocale, int sourceVersion,
            TranslationSource source, boolean original, UUID translatorUserId) {
        this.countryCode = countryCode;
        initLifecycle(id, locale, sourceLocale, sourceVersion, source, original, translatorUserId);
    }

    public void applyContent(String name, int sourceVersion, TranslationSource source, UUID editorUserId) {
        this.name = name;
        applyLifecycleOnEdit(sourceVersion, source, editorUserId);
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getName() {
        return name;
    }
}
