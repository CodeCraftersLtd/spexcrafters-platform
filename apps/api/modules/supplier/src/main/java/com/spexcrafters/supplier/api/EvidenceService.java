package com.spexcrafters.supplier.api;

import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.media.api.MalwareScanner;
import com.spexcrafters.media.api.ObjectNotFoundException;
import com.spexcrafters.media.api.ObjectStorage;
import com.spexcrafters.media.api.ObjectStream;
import com.spexcrafters.media.api.PresignedUpload;
import com.spexcrafters.platformaccess.api.PlatformAccess;
import com.spexcrafters.platformaccess.api.PlatformCapability;
import com.spexcrafters.sharedkernel.i18n.SupportedLocale;
import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import com.spexcrafters.sharedkernel.problem.ProblemFieldError;
import com.spexcrafters.sharedkernel.util.UuidV7;
import com.spexcrafters.supplier.domain.EvidenceMediaType;
import com.spexcrafters.supplier.domain.EvidenceStateException;
import com.spexcrafters.supplier.domain.ScanStatus;
import com.spexcrafters.supplier.domain.SupplierNotFoundException;
import com.spexcrafters.supplier.domain.VerificationEvidence;
import com.spexcrafters.supplier.infrastructure.VerificationEvidenceRepository;
import com.spexcrafters.supplier.infrastructure.config.EvidenceProperties;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evidence upload/finalize/list/delete/download (evidence-storage-architecture). Bytes go
 * directly to private object storage via a presigned PUT; the server generates the safe key
 * (never from the filename), then finalizes by validating presence, size, sha256 and magic
 * bytes. Downloads are re-authorized every time and fail-closed to downloadable states.
 */
@Service
public class EvidenceService {

    private final VerificationEvidenceRepository evidence;
    private final SupplierAccess access;
    private final PlatformAccess platformAccess;
    private final ReferenceCatalog referenceCatalog;
    private final ObjectStorage objectStorage;
    private final MalwareScanner malwareScanner;
    private final EvidenceProperties properties;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public EvidenceService(VerificationEvidenceRepository evidence,
            SupplierAccess access,
            PlatformAccess platformAccess,
            ReferenceCatalog referenceCatalog,
            ObjectStorage objectStorage,
            MalwareScanner malwareScanner,
            EvidenceProperties properties,
            AuditLogger auditLogger,
            Clock clock) {
        this.evidence = evidence;
        this.access = access;
        this.platformAccess = platformAccess;
        this.referenceCatalog = referenceCatalog;
        this.objectStorage = objectStorage;
        this.malwareScanner = malwareScanner;
        this.properties = properties;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Transactional
    public EvidenceUploadTicketDto initiateUpload(UUID userId, UUID supplierId, String evidenceTypeCode,
            String originalFilename, String declaredMediaType, String documentLocale) {
        SupplierContext context =
                access.requireForSupplier(userId, supplierId, SupplierCapability.EVIDENCE_UPLOAD);
        referenceCatalog.requireEvidenceType(evidenceTypeCode);
        if (EvidenceMediaType.fromDeclared(declaredMediaType).isEmpty()) {
            throw ApiProblemException.validation(List.of(new ProblemFieldError(
                    "mediaType", "UnsupportedMediaType",
                    "Only PDF, JPEG, PNG and WEBP are accepted.")));
        }
        String locale = documentLocale == null ? null : SupportedLocale.normalizeOrFallback(documentLocale);
        String safeFilename = sanitizeFilename(originalFilename);

        UUID evidenceId = UuidV7.generate();
        String storageKey = "evidence/" + context.organizationId() + "/" + supplierId + "/" + evidenceId;
        VerificationEvidence row = new VerificationEvidence(evidenceId, supplierId, context.organizationId(),
                evidenceTypeCode, storageKey, safeFilename, declaredMediaType.trim(), userId, locale);
        row.setCreatedBy(userId);
        row.setUpdatedBy(userId);
        evidence.save(row);

        PresignedUpload upload = objectStorage.presignUpload(
                storageKey, declaredMediaType.trim(), properties.maxBytes(), properties.presignTtl());

        auditLogger.record("supplier.evidence.upload_initiated", userId, "verification_evidence",
                evidenceId.toString(), Map.of("supplierId", supplierId.toString(),
                        "evidenceTypeCode", evidenceTypeCode));
        return new EvidenceUploadTicketDto(evidenceId, upload.method(), upload.url(), upload.expiresAt(),
                upload.requiredHeaders(), upload.maxBytes());
    }

    /**
     * Finalizes an initiated upload: verifies the object exists, enforces the size cap,
     * computes the sha256 and validates the magic bytes against the declared type. Idempotent
     * per evidence id. An optional {@code expectedSha256} is cross-checked (checksum mismatch
     * → rejected).
     */
    @Transactional
    public EvidenceDto finalizeUpload(UUID userId, UUID supplierId, UUID evidenceId, String expectedSha256) {
        access.requireForSupplier(userId, supplierId, SupplierCapability.EVIDENCE_UPLOAD);
        VerificationEvidence row = evidence.findByIdAndSupplierId(evidenceId, supplierId)
                .orElseThrow(SupplierNotFoundException::new);
        if (row.isFinalized()) {
            return toDto(row);
        }

        byte[] bytes;
        try {
            bytes = objectStorage.readAllBytes(row.getStorageKey());
        } catch (ObjectNotFoundException ex) {
            throw new EvidenceStateException("No uploaded object was found for this evidence.");
        }
        if (bytes.length > properties.maxBytes()) {
            objectStorage.delete(row.getStorageKey());
            evidence.delete(row);
            throw new EvidenceStateException("The uploaded file exceeds the maximum allowed size.");
        }
        Optional<EvidenceMediaType> declared = EvidenceMediaType.fromDeclared(row.getDeclaredMediaType());
        Optional<EvidenceMediaType> detected = EvidenceMediaType.detect(bytes);
        if (detected.isEmpty() || declared.isEmpty() || detected.get() != declared.get()) {
            row.applyScanVerdict(ScanStatus.REJECTED);
            objectStorage.delete(row.getStorageKey());
            evidence.delete(row);
            throw new EvidenceStateException(
                    "The file content does not match the declared type, or the type is not allowed.");
        }
        String sha256 = sha256Hex(bytes);
        if (expectedSha256 != null && !expectedSha256.equalsIgnoreCase(sha256)) {
            objectStorage.delete(row.getStorageKey());
            evidence.delete(row);
            throw new EvidenceStateException("The uploaded content checksum does not match.");
        }

        row.finalizeUpload(bytes.length, sha256, detected.get().mediaType(), clock.instant());
        row.setUpdatedBy(userId);
        MalwareScanner.ScanVerdict verdict = malwareScanner.scan(row.getStorageKey());
        row.applyScanVerdict(mapVerdict(verdict));

        auditLogger.record("supplier.evidence.finalized", userId, "verification_evidence",
                evidenceId.toString(), Map.of("supplierId", supplierId.toString(),
                        "sha256", sha256, "scanStatus", row.getScanStatus().name()));
        return toDto(row);
    }

    @Transactional(readOnly = true)
    public List<EvidenceDto> list(UUID userId, UUID supplierId) {
        access.requireForSupplier(userId, supplierId, SupplierCapability.EVIDENCE_READ);
        return evidence.findBySupplierIdAndUploadStateOrderByCreatedAtAsc(
                        supplierId, com.spexcrafters.supplier.domain.EvidenceUploadState.FINALIZED)
                .stream().map(EvidenceService::toDto).toList();
    }

    /** Finalized evidence for a caller authorized out-of-band (the reviewer workflow). */
    @Transactional(readOnly = true)
    public List<EvidenceDto> listForReview(UUID supplierId) {
        return evidence.findBySupplierIdAndUploadStateOrderByCreatedAtAsc(
                        supplierId, com.spexcrafters.supplier.domain.EvidenceUploadState.FINALIZED)
                .stream().map(EvidenceService::toDto).toList();
    }

    @Transactional
    public void delete(UUID userId, UUID supplierId, UUID evidenceId) {
        access.requireForSupplier(userId, supplierId, SupplierCapability.EVIDENCE_DELETE);
        VerificationEvidence row = evidence.findByIdAndSupplierId(evidenceId, supplierId)
                .orElseThrow(SupplierNotFoundException::new);
        if (!row.isDeletableBySupplier()) {
            throw new EvidenceStateException(
                    "This evidence is retained for a decided verification and cannot be deleted.");
        }
        objectStorage.delete(row.getStorageKey());
        evidence.delete(row);
        auditLogger.record("supplier.evidence.deleted", userId, "verification_evidence",
                evidenceId.toString(), Map.of("supplierId", supplierId.toString()));
    }

    /**
     * Authorizes and opens an evidence download. Supplier-org members with
     * {@code supplier.evidence.read} may fetch only their own org's evidence; active platform
     * staff with {@code supplier.review.read} may fetch any (audited). Non-owners get a
     * concealed 404. Fail-closed: only downloadable scan states are served.
     */
    @Transactional
    public EvidenceDownload download(UUID userId, UUID supplierId, UUID evidenceId) {
        boolean asStaff = platformAccess.isActiveStaff(userId);
        if (asStaff) {
            platformAccess.require(userId, PlatformCapability.REVIEW_READ);
        } else {
            access.requireForSupplier(userId, supplierId, SupplierCapability.EVIDENCE_READ);
        }
        VerificationEvidence row = evidence.findByIdAndSupplierId(evidenceId, supplierId)
                .orElseThrow(SupplierNotFoundException::new);
        if (!row.isDownloadable()) {
            throw new EvidenceStateException("This evidence is not available for download.");
        }
        ObjectStream stream;
        try {
            stream = objectStorage.open(row.getStorageKey());
        } catch (ObjectNotFoundException ex) {
            throw new SupplierNotFoundException();
        }
        auditLogger.record("supplier.evidence.downloaded", userId, "verification_evidence",
                evidenceId.toString(), Map.of("supplierId", supplierId.toString(),
                        "asStaff", Boolean.toString(asStaff)));
        return new EvidenceDownload(stream.stream(), stream.contentLength(),
                row.getMediaType() != null ? row.getMediaType() : stream.contentType(), row.getOriginalFilename());
    }

    // ------------------------------------------------- cross-module linkage (verification)

    /**
     * Looks up finalized evidence owned by {@code supplierId}, for the verification context to
     * validate evidence linkage on a scope grant. Authorization of the grant itself is the
     * verification context's responsibility (platform staff).
     */
    @Transactional(readOnly = true)
    public Optional<EvidenceRef> findForSupplier(UUID evidenceId, UUID supplierId) {
        return evidence.findByIdAndSupplierId(evidenceId, supplierId)
                .map(e -> new EvidenceRef(e.getId(), e.getSupplierId(), e.getEvidenceTypeCode(), e.isFinalized()));
    }

    /** Marks evidence retained because a verification decision now depends on it. */
    @Transactional
    public void retain(UUID evidenceId) {
        evidence.findById(evidenceId).ifPresent(VerificationEvidence::markRetained);
    }

    // ---------------------------------------------------------------- internals

    private static ScanStatus mapVerdict(MalwareScanner.ScanVerdict verdict) {
        return switch (verdict) {
            case PENDING -> ScanStatus.PENDING_SCAN;
            case CLEAN -> ScanStatus.CLEAN;
            case QUARANTINED -> ScanStatus.QUARANTINED;
            case REJECTED -> ScanStatus.REJECTED;
        };
    }

    /** Strips any path components and control characters; the safe key never uses this value. */
    static String sanitizeFilename(String raw) {
        if (raw == null || raw.isBlank()) {
            return "evidence";
        }
        String base = raw.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[\\p{Cntrl}]", "").trim();
        base = base.replaceAll("[^A-Za-z0-9._ -]", "_");
        if (base.isBlank() || base.equals(".") || base.equals("..")) {
            return "evidence";
        }
        return base.length() > 200 ? base.substring(0, 200) : base;
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required but unavailable", ex);
        }
    }

    private static EvidenceDto toDto(VerificationEvidence e) {
        return new EvidenceDto(e.getId(), e.getSupplierId(), e.getEvidenceTypeCode(), e.getOriginalFilename(),
                e.getMediaType(), e.getByteSize(), e.getSha256(), e.getDocumentLocale(), e.getUploadState(),
                e.getScanStatus(), e.getReviewStatus(), e.getRetentionStatus(), e.isDownloadable(),
                e.getUploadedAt());
    }
}
