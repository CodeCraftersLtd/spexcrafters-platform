package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;

/**
 * The ADR-020 translation-row lifecycle shape, shared by every {@code *_translation} table in
 * the taxonomy registry (mirrors {@code supplier.SupplierProfileTranslation}). Concrete
 * subclasses add the parent FK column and the translatable text columns of their table.
 *
 * <p>The original-language row ({@code isOriginal}) is authoritative and never overwritten by a
 * translation. A non-original row whose {@code sourceVersion} lags the parent's current source
 * version is <em>stale</em>. Only {@code isOriginal} or {@code APPROVED} rows are shown publicly.
 */
@MappedSuperclass
public abstract class TaxonomyTranslation extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "locale", nullable = false, length = 16)
    private String locale;

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

    @Column(name = "is_original", nullable = false)
    private boolean original;

    protected TaxonomyTranslation() {
        // JPA
    }

    /** Initializes the shared lifecycle fields; called by subclass constructors. */
    protected void initLifecycle(UUID id, String locale, String sourceLocale, int sourceVersion,
            TranslationSource source, boolean original, UUID translatorUserId) {
        this.id = id;
        this.locale = locale;
        this.sourceLocale = sourceLocale;
        this.sourceVersion = sourceVersion;
        this.translationSource = source;
        this.original = original;
        this.translatorUserId = translatorUserId;
        this.translationStatus = original ? TranslationStatus.APPROVED : TranslationStatus.DRAFT;
    }

    /**
     * Records that content was (re)written against {@code sourceVersion} with the given source.
     * For a non-original row this resets approval (DRAFT, or MACHINE_TRANSLATED for machine
     * source); the original row stays authoritative/APPROVED. Subclasses set their text columns
     * then call this.
     */
    protected void applyLifecycleOnEdit(int sourceVersion, TranslationSource source, UUID editorUserId) {
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

    /** A translation is stale when its source version lags the parent's current source version. */
    public boolean isStale(int currentParentSourceVersion) {
        return !original && sourceVersion < currentParentSourceVersion;
    }

    /** Displayable publicly: the authoritative original, or an approved translation. */
    public boolean isDisplayable() {
        return original || translationStatus == TranslationStatus.APPROVED;
    }

    public UUID getId() {
        return id;
    }

    public String getLocale() {
        return locale;
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
