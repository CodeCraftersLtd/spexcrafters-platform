package com.spexcrafters.supplier.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A per-locale translation of a {@link SupplierFacility}'s name/description (ADR-020). One row
 * per {@code (facility_id, locale)}; the original-language row is authoritative.
 */
@Entity
@Table(name = "supplier_facility_translation", schema = "supplier")
public class SupplierFacilityTranslation extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "facility_id", nullable = false)
    private UUID facilityId;

    @Column(name = "locale", nullable = false, length = 16)
    private String locale;

    @Column(name = "name", length = 300)
    private String name;

    @Column(name = "description", length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "translation_status", nullable = false, length = 32)
    private TranslationStatus translationStatus;

    @Column(name = "source_version", nullable = false)
    private int sourceVersion;

    @Column(name = "is_original", nullable = false)
    private boolean original;

    protected SupplierFacilityTranslation() {
        // JPA
    }

    public SupplierFacilityTranslation(UUID id, UUID facilityId, String locale, int sourceVersion,
            boolean original) {
        this.id = id;
        this.facilityId = facilityId;
        this.locale = locale;
        this.sourceVersion = sourceVersion;
        this.original = original;
        this.translationStatus = original ? TranslationStatus.APPROVED : TranslationStatus.DRAFT;
    }

    public void applyContent(String name, String description, int sourceVersion) {
        this.name = name;
        this.description = description;
        this.sourceVersion = sourceVersion;
        if (!original) {
            this.translationStatus = TranslationStatus.DRAFT;
        }
    }

    public boolean isStale(int currentFacilitySourceVersion) {
        return !original && sourceVersion < currentFacilitySourceVersion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFacilityId() {
        return facilityId;
    }

    public String getLocale() {
        return locale;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public TranslationStatus getTranslationStatus() {
        return translationStatus;
    }

    public int getSourceVersion() {
        return sourceVersion;
    }

    public boolean isOriginal() {
        return original;
    }
}
