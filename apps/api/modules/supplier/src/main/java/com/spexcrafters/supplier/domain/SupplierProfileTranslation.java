package com.spexcrafters.supplier.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A per-locale translation of a {@link SupplierProfile}'s class-D content (ADR-020). One row
 * per {@code (profile_id, locale)}. The original-language row is authoritative and is never
 * overwritten by another locale's translation. Carries the ADR-020 lifecycle columns and the
 * {@code sourceVersion} the content was translated against (for stale detection).
 */
@Entity
@Table(name = "supplier_profile_translation", schema = "supplier")
public class SupplierProfileTranslation extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "locale", nullable = false, length = 16)
    private String locale;

    @Column(name = "trading_name", length = 300)
    private String tradingName;

    @Column(name = "company_description", length = 4000)
    private String companyDescription;

    @Column(name = "production_capability_description", length = 4000)
    private String productionCapabilityDescription;

    @Column(name = "oem_description", length = 4000)
    private String oemDescription;

    @Column(name = "odm_description", length = 4000)
    private String odmDescription;

    @Column(name = "private_label_description", length = 4000)
    private String privateLabelDescription;

    @Column(name = "quality_control_description", length = 4000)
    private String qualityControlDescription;

    @Column(name = "export_market_description", length = 4000)
    private String exportMarketDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "translation_status", nullable = false, length = 32)
    private TranslationStatus translationStatus;

    @Column(name = "source_locale", nullable = false, length = 16)
    private String sourceLocale;

    @Column(name = "source_version", nullable = false)
    private int sourceVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "translation_source", nullable = false, length = 16)
    private TranslationSource translationSource;

    @Column(name = "translator_user_id")
    private UUID translatorUserId;

    @Column(name = "reviewer_user_id")
    private UUID reviewerUserId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    /** True for the original-language row: authoritative, never overwritten by a translation. */
    @Column(name = "is_original", nullable = false)
    private boolean original;

    protected SupplierProfileTranslation() {
        // JPA
    }

    public SupplierProfileTranslation(UUID id, UUID profileId, String locale, String sourceLocale,
            int sourceVersion, TranslationSource translationSource, boolean original, UUID translatorUserId) {
        this.id = id;
        this.profileId = profileId;
        this.locale = locale;
        this.sourceLocale = sourceLocale;
        this.sourceVersion = sourceVersion;
        this.translationSource = translationSource;
        this.original = original;
        this.translatorUserId = translatorUserId;
        this.translationStatus = original ? TranslationStatus.APPROVED : TranslationStatus.DRAFT;
    }

    /** Applies translated content and records the source version it targets. Resets approval. */
    public void applyContent(String tradingName, String companyDescription,
            String productionCapabilityDescription, String oemDescription, String odmDescription,
            String privateLabelDescription, String qualityControlDescription,
            String exportMarketDescription, int sourceVersion, TranslationSource source, UUID editorUserId) {
        this.tradingName = tradingName;
        this.companyDescription = companyDescription;
        this.productionCapabilityDescription = productionCapabilityDescription;
        this.oemDescription = oemDescription;
        this.odmDescription = odmDescription;
        this.privateLabelDescription = privateLabelDescription;
        this.qualityControlDescription = qualityControlDescription;
        this.exportMarketDescription = exportMarketDescription;
        this.sourceVersion = sourceVersion;
        this.translationSource = source;
        this.translatorUserId = editorUserId;
        if (!original) {
            this.translationStatus = source == TranslationSource.MACHINE
                    ? TranslationStatus.MACHINE_TRANSLATED : TranslationStatus.DRAFT;
            this.approvedAt = null;
            this.approvedBy = null;
            this.reviewerUserId = null;
        }
    }

    public void approve(UUID reviewerUserId, Instant now) {
        this.translationStatus = TranslationStatus.APPROVED;
        this.reviewerUserId = reviewerUserId;
        this.approvedBy = reviewerUserId;
        this.approvedAt = now;
    }

    public void reject(UUID reviewerUserId) {
        this.translationStatus = TranslationStatus.REJECTED;
        this.reviewerUserId = reviewerUserId;
        this.approvedAt = null;
        this.approvedBy = null;
    }

    /** A translation is stale when its source version lags the profile's current version. */
    public boolean isStale(int currentProfileSourceVersion) {
        return !original && sourceVersion < currentProfileSourceVersion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public String getLocale() {
        return locale;
    }

    public String getTradingName() {
        return tradingName;
    }

    public String getCompanyDescription() {
        return companyDescription;
    }

    public String getProductionCapabilityDescription() {
        return productionCapabilityDescription;
    }

    public String getOemDescription() {
        return oemDescription;
    }

    public String getOdmDescription() {
        return odmDescription;
    }

    public String getPrivateLabelDescription() {
        return privateLabelDescription;
    }

    public String getQualityControlDescription() {
        return qualityControlDescription;
    }

    public String getExportMarketDescription() {
        return exportMarketDescription;
    }

    public TranslationStatus getTranslationStatus() {
        return translationStatus;
    }

    public String getSourceLocale() {
        return sourceLocale;
    }

    public int getSourceVersion() {
        return sourceVersion;
    }

    public TranslationSource getTranslationSource() {
        return translationSource;
    }

    public boolean isOriginal() {
        return original;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }
}
