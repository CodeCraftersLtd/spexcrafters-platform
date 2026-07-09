package com.spexcrafters.supplier.api;

import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.platformaccess.api.PlatformAccess;
import com.spexcrafters.platformaccess.api.PlatformCapability;
import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import com.spexcrafters.sharedkernel.problem.ProblemFieldError;
import com.spexcrafters.sharedkernel.util.UuidV7;
import com.spexcrafters.supplier.domain.ApplicationStatus;
import com.spexcrafters.supplier.domain.ReviewRequest;
import com.spexcrafters.supplier.domain.Supplier;
import com.spexcrafters.supplier.domain.SupplierApplication;
import com.spexcrafters.supplier.domain.SupplierNotFoundException;
import com.spexcrafters.supplier.domain.SupplierProfile;
import com.spexcrafters.supplier.infrastructure.ReviewRequestRepository;
import com.spexcrafters.supplier.infrastructure.SupplierApplicationRepository;
import com.spexcrafters.supplier.infrastructure.SupplierProfileRepository;
import com.spexcrafters.supplier.infrastructure.SupplierRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The reviewer (platform-staff) workflow (supplier-domain-model §4, §6): queue, detail, claim,
 * request-changes, approve (which activates the supplier in the same transaction), reject and
 * platform suspension. Every action is authorized against a platform capability — an
 * organization role, however senior, can never reach these.
 */
@Service
public class SupplierReviewService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final List<ApplicationStatus> QUEUE_STATUSES = List.of(
            ApplicationStatus.SUBMITTED, ApplicationStatus.RESUBMITTED,
            ApplicationStatus.UNDER_REVIEW, ApplicationStatus.CHANGES_REQUESTED);

    private final SupplierApplicationRepository applications;
    private final SupplierRepository suppliers;
    private final SupplierProfileRepository profiles;
    private final ReviewRequestRepository reviewRequests;
    private final SupplierProfileService profileService;
    private final EvidenceService evidenceService;
    private final PlatformAccess platformAccess;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public SupplierReviewService(SupplierApplicationRepository applications,
            SupplierRepository suppliers,
            SupplierProfileRepository profiles,
            ReviewRequestRepository reviewRequests,
            SupplierProfileService profileService,
            EvidenceService evidenceService,
            PlatformAccess platformAccess,
            AuditLogger auditLogger,
            Clock clock) {
        this.applications = applications;
        this.suppliers = suppliers;
        this.profiles = profiles;
        this.reviewRequests = reviewRequests;
        this.profileService = profileService;
        this.evidenceService = evidenceService;
        this.platformAccess = platformAccess;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ReviewQueuePageDto queue(UUID userId, String cursor, Integer requestedSize) {
        platformAccess.require(userId, PlatformCapability.REVIEW_READ);
        int size = requestedSize == null ? 20 : Math.min(Math.max(requestedSize, 1), MAX_PAGE_SIZE);
        Limit limit = Limit.of(size + 1);
        List<SupplierApplication> page = cursor == null
                ? applications.findByStatusInOrderByIdAsc(QUEUE_STATUSES, limit)
                : applications.findByStatusInAndIdGreaterThanOrderByIdAsc(
                        QUEUE_STATUSES, parseCursor(cursor), limit);
        boolean hasMore = page.size() > size;
        List<SupplierApplication> items = hasMore ? page.subList(0, size) : page;
        List<ReviewQueueItemDto> dtos = items.stream().map(this::toQueueItem).toList();
        String next = hasMore ? items.get(items.size() - 1).getId().toString() : null;
        return new ReviewQueuePageDto(dtos, next);
    }

    @Transactional(readOnly = true)
    public ReviewDetailDto detail(UUID userId, UUID applicationId) {
        platformAccess.require(userId, PlatformCapability.REVIEW_READ);
        SupplierApplication application = loadApplication(applicationId);
        Supplier supplier = loadSupplier(application.getSupplierId());
        SupplierProfileDto profile = profileService.buildProfileDto(supplier);
        List<EvidenceDto> evidence = evidenceService.listForReview(supplier.getId());
        List<ReviewRequestDto> changeRequests = reviewRequests
                .findByApplicationIdOrderByRequestedAtAsc(applicationId).stream()
                .map(SupplierReviewService::toReviewRequestDto).toList();
        return new ReviewDetailDto(application.getId(), supplier.getId(), supplier.getOrganizationId(),
                application.getStatus(), supplier.getOperationalStatus(), application.getSubmittedAt(),
                application.getVersion(), profile, evidence, changeRequests);
    }

    @Transactional
    public ReviewDetailDto claim(UUID userId, UUID applicationId) {
        platformAccess.require(userId, PlatformCapability.REVIEW_CLAIM);
        SupplierApplication application = loadApplication(applicationId);
        application.claimForReview(userId);
        application.setUpdatedBy(userId);
        auditLogger.record("supplier.review.claimed", userId, "supplier_application",
                applicationId.toString());
        return detailAfterMutation(application);
    }

    @Transactional
    public ReviewDetailDto requestChanges(UUID userId, UUID applicationId, String requestedItem, String reason) {
        platformAccess.require(userId, PlatformCapability.REVIEW_REQUEST_CHANGES);
        if (requestedItem == null || requestedItem.isBlank() || reason == null || reason.isBlank()) {
            throw ApiProblemException.validation(List.of(new ProblemFieldError(
                    "reason", "Required", "A requested item and reason are required.")));
        }
        SupplierApplication application = loadApplication(applicationId);
        application.requestChanges();
        application.setUpdatedBy(userId);
        ReviewRequest request = new ReviewRequest(UuidV7.generate(), applicationId, requestedItem.trim(),
                reason.trim(), userId, clock.instant());
        request.setCreatedBy(userId);
        request.setUpdatedBy(userId);
        reviewRequests.save(request);
        auditLogger.record("supplier.application.changes_requested", userId, "supplier_application",
                applicationId.toString(), Map.of("requestedItem", requestedItem.trim()));
        auditLogger.record("supplier.review.decision_recorded", userId, "supplier_application",
                applicationId.toString(), Map.of("decision", "CHANGES_REQUESTED"));
        return detailAfterMutation(application);
    }

    @Transactional
    public ReviewDetailDto approve(UUID userId, UUID applicationId) {
        platformAccess.require(userId, PlatformCapability.REVIEW_APPROVE);
        SupplierApplication application = loadApplication(applicationId);
        Supplier supplier = loadSupplier(application.getSupplierId());
        application.approve(userId, clock.instant());
        application.setUpdatedBy(userId);
        supplier.activate();
        supplier.setUpdatedBy(userId);
        auditLogger.record("supplier.application.approved", userId, "supplier_application",
                applicationId.toString(), Map.of("supplierId", supplier.getId().toString()));
        auditLogger.record("supplier.activated", userId, "supplier", supplier.getId().toString());
        auditLogger.record("supplier.review.decision_recorded", userId, "supplier_application",
                applicationId.toString(), Map.of("decision", "APPROVED"));
        return detailAfterMutation(application);
    }

    @Transactional
    public ReviewDetailDto reject(UUID userId, UUID applicationId, String reason) {
        platformAccess.require(userId, PlatformCapability.REVIEW_REJECT);
        SupplierApplication application = loadApplication(applicationId);
        Supplier supplier = loadSupplier(application.getSupplierId());
        application.reject(userId, clock.instant());
        application.setUpdatedBy(userId);
        supplier.deactivate();
        supplier.setUpdatedBy(userId);
        auditLogger.record("supplier.application.rejected", userId, "supplier_application",
                applicationId.toString(), reason == null ? Map.of() : Map.of("reason", reason));
        auditLogger.record("supplier.review.decision_recorded", userId, "supplier_application",
                applicationId.toString(), Map.of("decision", "REJECTED"));
        return detailAfterMutation(application);
    }

    @Transactional
    public void suspendSupplier(UUID userId, UUID supplierId, String reason) {
        platformAccess.require(userId, PlatformCapability.SUPPLIER_SUSPEND);
        Supplier supplier = loadSupplier(supplierId);
        supplier.suspend();
        supplier.setUpdatedBy(userId);
        auditLogger.record("supplier.suspended", userId, "supplier", supplierId.toString(),
                reason == null ? Map.of() : Map.of("reason", reason));
    }

    // ---------------------------------------------------------------- internals

    private ReviewDetailDto detailAfterMutation(SupplierApplication application) {
        Supplier supplier = loadSupplier(application.getSupplierId());
        SupplierProfileDto profile = profileService.buildProfileDto(supplier);
        List<EvidenceDto> evidence = evidenceService.listForReview(supplier.getId());
        List<ReviewRequestDto> changeRequests = reviewRequests
                .findByApplicationIdOrderByRequestedAtAsc(application.getId()).stream()
                .map(SupplierReviewService::toReviewRequestDto).toList();
        return new ReviewDetailDto(application.getId(), supplier.getId(), supplier.getOrganizationId(),
                application.getStatus(), supplier.getOperationalStatus(), application.getSubmittedAt(),
                application.getVersion(), profile, evidence, changeRequests);
    }

    private ReviewQueueItemDto toQueueItem(SupplierApplication application) {
        Supplier supplier = loadSupplier(application.getSupplierId());
        SupplierProfile profile = profiles.findBySupplierId(supplier.getId()).orElse(null);
        String legalName = profile == null ? null : profile.getLegalName();
        return new ReviewQueueItemDto(application.getId(), supplier.getId(), supplier.getOrganizationId(),
                application.getStatus(), legalName, supplier.getOriginalLocale(), application.getSubmittedAt());
    }

    private static ReviewRequestDto toReviewRequestDto(ReviewRequest r) {
        return new ReviewRequestDto(r.getId(), r.getApplicationId(), r.getRequestedItem(), r.getReason(),
                r.getStatus(), r.getSupplierResponse(), r.getResponseLocale(), r.getRequestedAt(),
                r.getResolvedAt());
    }

    private SupplierApplication loadApplication(UUID applicationId) {
        return applications.findById(applicationId).orElseThrow(SupplierNotFoundException::new);
    }

    private Supplier loadSupplier(UUID supplierId) {
        return suppliers.findById(supplierId).orElseThrow(SupplierNotFoundException::new);
    }

    private static UUID parseCursor(String cursor) {
        try {
            return UUID.fromString(cursor);
        } catch (IllegalArgumentException ex) {
            throw ApiProblemException.validation(List.of(new ProblemFieldError(
                    "cursor", "InvalidCursor", "The pagination cursor is invalid.")));
        }
    }
}
