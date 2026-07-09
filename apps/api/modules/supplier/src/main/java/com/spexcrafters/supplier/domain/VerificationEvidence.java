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
 * Metadata for a piece of verification evidence (evidence-storage-architecture). Bytes live
 * in object storage; only this metadata is in PostgreSQL. The row is created in
 * {@code INITIATED} state at upload-initiate and moves to {@code FINALIZED} once the server
 * verifies presence, size, sha256 and magic bytes. Evidence is org- and supplier-scoped for
 * tenancy isolation and IDOR concealment; the {@code storageKey} embeds a server-generated
 * UUIDv7, never a client filename.
 */
@Entity
@Table(name = "verification_evidence", schema = "supplier")
public class VerificationEvidence extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "evidence_type_code", nullable = false, length = 64)
    private String evidenceTypeCode;

    @Column(name = "storage_key", nullable = false, length = 300)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 300)
    private String originalFilename;

    @Column(name = "declared_media_type", nullable = false, length = 100)
    private String declaredMediaType;

    @Column(name = "media_type", length = 100)
    private String mediaType;

    @Column(name = "byte_size")
    private Long byteSize;

    @Column(name = "sha256", length = 64)
    private String sha256;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_state", nullable = false, length = 32)
    private EvidenceUploadState uploadState;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_status", nullable = false, length = 32)
    private ScanStatus scanStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 32)
    private EvidenceReviewStatus reviewStatus;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "document_locale", length = 16)
    private String documentLocale;

    @Enumerated(EnumType.STRING)
    @Column(name = "retention_status", nullable = false, length = 32)
    private RetentionStatus retentionStatus;

    protected VerificationEvidence() {
        // JPA
    }

    public VerificationEvidence(UUID id, UUID supplierId, UUID organizationId, String evidenceTypeCode,
            String storageKey, String originalFilename, String declaredMediaType, UUID uploadedBy,
            String documentLocale) {
        this.id = id;
        this.supplierId = supplierId;
        this.organizationId = organizationId;
        this.evidenceTypeCode = evidenceTypeCode;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.declaredMediaType = declaredMediaType;
        this.uploadedBy = uploadedBy;
        this.documentLocale = documentLocale;
        this.uploadState = EvidenceUploadState.INITIATED;
        this.scanStatus = ScanStatus.PENDING_SCAN;
        this.reviewStatus = EvidenceReviewStatus.UNREVIEWED;
        this.retentionStatus = RetentionStatus.NONE;
    }

    /**
     * Records the server-validated bytes' facts and moves to {@code FINALIZED}. Idempotent:
     * re-finalizing already-finalized evidence with matching facts is a no-op for the caller.
     */
    public void finalizeUpload(long byteSize, String sha256, String detectedMediaType, Instant now) {
        this.byteSize = byteSize;
        this.sha256 = sha256;
        this.mediaType = detectedMediaType;
        this.uploadedAt = now;
        this.uploadState = EvidenceUploadState.FINALIZED;
        this.scanStatus = ScanStatus.PENDING_SCAN;
    }

    public void applyScanVerdict(ScanStatus verdict) {
        this.scanStatus = verdict;
    }

    public void markRetained() {
        this.retentionStatus = RetentionStatus.RETAINED;
    }

    public boolean isFinalized() {
        return uploadState == EvidenceUploadState.FINALIZED;
    }

    public boolean isDownloadable() {
        return isFinalized() && scanStatus.isDownloadable();
    }

    /** Only unreferenced, unretained, unreviewed evidence may be deleted by the supplier. */
    public boolean isDeletableBySupplier() {
        return retentionStatus == RetentionStatus.NONE && reviewStatus == EvidenceReviewStatus.UNREVIEWED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getEvidenceTypeCode() {
        return evidenceTypeCode;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getDeclaredMediaType() {
        return declaredMediaType;
    }

    public String getMediaType() {
        return mediaType;
    }

    public Long getByteSize() {
        return byteSize;
    }

    public String getSha256() {
        return sha256;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public EvidenceUploadState getUploadState() {
        return uploadState;
    }

    public ScanStatus getScanStatus() {
        return scanStatus;
    }

    public EvidenceReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public String getDocumentLocale() {
        return documentLocale;
    }

    public RetentionStatus getRetentionStatus() {
        return retentionStatus;
    }
}
