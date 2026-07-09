package com.spexcrafters.supplier.api;

import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.organizations.api.OrgMembershipView;
import com.spexcrafters.sharedkernel.i18n.SupportedLocale;
import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import com.spexcrafters.sharedkernel.problem.ConflictException;
import com.spexcrafters.sharedkernel.problem.ProblemFieldError;
import com.spexcrafters.sharedkernel.util.UuidV7;
import com.spexcrafters.supplier.domain.ApplicationStatus;
import com.spexcrafters.supplier.domain.ReviewRequest;
import com.spexcrafters.supplier.domain.SupplierApplication;
import com.spexcrafters.supplier.domain.OneActiveSupplierException;
import com.spexcrafters.supplier.domain.OperationalStatus;
import com.spexcrafters.supplier.domain.Supplier;
import com.spexcrafters.supplier.domain.SupplierCapabilityDeclaration;
import com.spexcrafters.supplier.domain.SupplierNotFoundException;
import com.spexcrafters.supplier.domain.SupplierProfile;
import com.spexcrafters.supplier.domain.SupplierProfileTranslation;
import com.spexcrafters.supplier.domain.SupplierTypeAssignment;
import com.spexcrafters.supplier.domain.TranslationSource;
import com.spexcrafters.supplier.infrastructure.ReviewRequestRepository;
import com.spexcrafters.supplier.infrastructure.SupplierApplicationRepository;
import com.spexcrafters.supplier.infrastructure.SupplierCapabilityDeclarationRepository;
import com.spexcrafters.supplier.infrastructure.SupplierProfileRepository;
import com.spexcrafters.supplier.infrastructure.SupplierProfileTranslationRepository;
import com.spexcrafters.supplier.infrastructure.SupplierRepository;
import com.spexcrafters.supplier.infrastructure.SupplierTypeAssignmentRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supplier application lifecycle and draft content (supplier-domain-model §4). Owns atomic
 * creation (supplier + application + profile + original-language translation with the
 * one-active-supplier guard), draft editing, submit-with-completeness, withdraw and resubmit.
 * Reviewer-side transitions live in {@link SupplierReviewService}.
 */
@Service
public class SupplierApplicationService {

    private final SupplierRepository suppliers;
    private final SupplierApplicationRepository applications;
    private final SupplierProfileRepository profiles;
    private final SupplierProfileTranslationRepository profileTranslations;
    private final SupplierTypeAssignmentRepository typeAssignments;
    private final SupplierCapabilityDeclarationRepository capabilityDeclarations;
    private final ReviewRequestRepository reviewRequests;
    private final SupplierAccess access;
    private final ReferenceCatalog referenceCatalog;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public SupplierApplicationService(SupplierRepository suppliers,
            SupplierApplicationRepository applications,
            SupplierProfileRepository profiles,
            SupplierProfileTranslationRepository profileTranslations,
            SupplierTypeAssignmentRepository typeAssignments,
            SupplierCapabilityDeclarationRepository capabilityDeclarations,
            ReviewRequestRepository reviewRequests,
            SupplierAccess access,
            ReferenceCatalog referenceCatalog,
            AuditLogger auditLogger,
            Clock clock) {
        this.suppliers = suppliers;
        this.applications = applications;
        this.profiles = profiles;
        this.profileTranslations = profileTranslations;
        this.typeAssignments = typeAssignments;
        this.capabilityDeclarations = capabilityDeclarations;
        this.reviewRequests = reviewRequests;
        this.access = access;
        this.referenceCatalog = referenceCatalog;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    /**
     * Creates the supplier identity, its DRAFT application, an empty profile and the
     * original-language translation row — atomically. The one-active-supplier invariant is
     * pre-checked for a friendly error and hard-enforced by the partial unique index.
     */
    @Transactional
    public SupplierApplicationDto create(UUID userId, UUID organizationId, String originalLocale,
            String legalName) {
        OrgMembershipView membership =
                access.requireForOrganization(userId, organizationId, SupplierCapability.CREATE);
        String locale = normalizeRequiredLocale(originalLocale);

        if (suppliers.findFirstByOrganizationIdAndOperationalStatusNot(
                organizationId, OperationalStatus.DEACTIVATED).isPresent()) {
            throw new OneActiveSupplierException();
        }

        Supplier supplier = new Supplier(UuidV7.generate(), organizationId, locale);
        supplier.setCreatedBy(userId);
        supplier.setUpdatedBy(userId);
        try {
            suppliers.saveAndFlush(supplier);
        } catch (DataIntegrityViolationException ex) {
            throw new OneActiveSupplierException();
        }

        SupplierProfile profile = new SupplierProfile(UuidV7.generate(), supplier.getId(), legalName);
        profile.setCreatedBy(userId);
        profile.setUpdatedBy(userId);
        profiles.save(profile);

        SupplierProfileTranslation original = new SupplierProfileTranslation(
                UuidV7.generate(), profile.getId(), locale, locale, profile.getSourceVersion(),
                TranslationSource.HUMAN, true, userId);
        original.setCreatedBy(userId);
        original.setUpdatedBy(userId);
        profileTranslations.save(original);

        SupplierApplication application = new SupplierApplication(UuidV7.generate(), supplier.getId());
        application.setCreatedBy(userId);
        application.setUpdatedBy(userId);
        applications.save(application);

        auditLogger.record("supplier.application.created", userId, "supplier_application",
                application.getId().toString(),
                java.util.Map.of("supplierId", supplier.getId().toString(),
                        "organizationId", organizationId.toString()));
        return toDto(application, supplier, membership.role());
    }

    @Transactional(readOnly = true)
    public SupplierApplicationDto get(UUID userId, UUID applicationId) {
        SupplierApplication application = loadApplication(applicationId);
        SupplierContext context =
                access.requireForSupplier(userId, application.getSupplierId(), SupplierCapability.READ);
        return toDto(application, context.supplier(), context.role());
    }

    /**
     * Applies a draft content update (profile class-E fields, original-language trading name,
     * declared types and capabilities). Only editable states (DRAFT, CHANGES_REQUESTED) may be
     * updated. Guarded by the application's optimistic {@code version}. A change to the
     * translatable trading name bumps the profile source version (marks translations stale).
     */
    @Transactional
    public SupplierApplicationDto updateDraft(UUID userId, UUID applicationId, DraftUpdate update,
            int expectedVersion) {
        SupplierApplication application = loadApplication(applicationId);
        SupplierContext context =
                access.requireForSupplier(userId, application.getSupplierId(), SupplierCapability.UPDATE);
        if (!application.isEditableBySupplier()) {
            throw new ApiProblemException(org.springframework.http.HttpStatus.CONFLICT,
                    com.spexcrafters.supplier.domain.SupplierProblemTypes.INVALID_APPLICATION_STATE,
                    "Application not editable",
                    "The application can only be edited while in DRAFT or CHANGES_REQUESTED.");
        }
        if (application.getVersion() != expectedVersion) {
            throw new ConflictException(
                    "The application was modified by someone else. Reload it and retry with the current version.");
        }

        Supplier supplier = context.supplier();
        SupplierProfile profile = profiles.findBySupplierId(supplier.getId()).orElseThrow();
        applyProfileFields(profile, update, userId);
        applyTypes(supplier.getId(), update.types(), userId);
        applyCapabilities(supplier.getId(), update.capabilities(), userId);

        application.setUpdatedBy(userId);
        auditLogger.record("supplier.application.updated", userId, "supplier_application",
                application.getId().toString());
        return toDto(application, supplier, context.role());
    }

    @Transactional
    public SupplierApplicationDto submit(UUID userId, UUID applicationId) {
        SupplierApplication application = loadApplication(applicationId);
        SupplierContext context =
                access.requireForSupplier(userId, application.getSupplierId(), SupplierCapability.SUBMIT);
        requireComplete(context.supplier());
        boolean firstSubmit = application.getStatus() == ApplicationStatus.DRAFT;
        if (firstSubmit) {
            application.submit(clock.instant());
        } else {
            application.resubmit(clock.instant());
        }
        application.setUpdatedBy(userId);
        auditLogger.record(firstSubmit ? "supplier.application.submitted"
                        : "supplier.application.resubmitted",
                userId, "supplier_application", application.getId().toString());
        return toDto(application, context.supplier(), context.role());
    }

    @Transactional
    public SupplierApplicationDto withdraw(UUID userId, UUID applicationId) {
        SupplierApplication application = loadApplication(applicationId);
        SupplierContext context =
                access.requireForSupplier(userId, application.getSupplierId(), SupplierCapability.WITHDRAW);
        application.withdraw();
        application.setUpdatedBy(userId);
        Supplier supplier = context.supplier();
        supplier.deactivate();
        supplier.setUpdatedBy(userId);
        auditLogger.record("supplier.application.withdrawn", userId, "supplier_application",
                application.getId().toString());
        return toDto(application, supplier, context.role());
    }

    @Transactional(readOnly = true)
    public List<ReviewRequestDto> listChangeRequests(UUID userId, UUID applicationId) {
        SupplierApplication application = loadApplication(applicationId);
        access.requireForSupplier(userId, application.getSupplierId(), SupplierCapability.READ);
        return reviewRequests.findByApplicationIdOrderByRequestedAtAsc(applicationId).stream()
                .map(SupplierApplicationService::toReviewRequestDto).toList();
    }

    /** The supplier org responds to an open change request (class-D content with its locale). */
    @Transactional
    public ReviewRequestDto respondToChangeRequest(UUID userId, UUID applicationId, UUID changeRequestId,
            String response, String responseLocale) {
        SupplierApplication application = loadApplication(applicationId);
        access.requireForSupplier(userId, application.getSupplierId(), SupplierCapability.UPDATE);
        ReviewRequest request = reviewRequests.findByIdAndApplicationId(changeRequestId, applicationId)
                .orElseThrow(SupplierNotFoundException::new);
        if (response == null || response.isBlank()) {
            throw ApiProblemException.validation(List.of(
                    new ProblemFieldError("response", "Required", "A response is required.")));
        }
        String locale = responseLocale == null ? null : SupportedLocale.normalizeOrFallback(responseLocale);
        request.respond(response.trim(), locale);
        request.setUpdatedBy(userId);
        auditLogger.record("supplier.review.change_response_recorded", userId, "review_request",
                changeRequestId.toString(), java.util.Map.of("applicationId", applicationId.toString()));
        return toReviewRequestDto(request);
    }

    private static ReviewRequestDto toReviewRequestDto(ReviewRequest r) {
        return new ReviewRequestDto(r.getId(), r.getApplicationId(), r.getRequestedItem(), r.getReason(),
                r.getStatus(), r.getSupplierResponse(), r.getResponseLocale(), r.getRequestedAt(),
                r.getResolvedAt());
    }

    // ---------------------------------------------------------------- internals

    private SupplierApplication loadApplication(UUID applicationId) {
        return applications.findById(applicationId).orElseThrow(SupplierNotFoundException::new);
    }

    private String normalizeRequiredLocale(String raw) {
        return SupportedLocale.parse(raw)
                .orElseThrow(() -> ApiProblemException.validation(List.of(
                        new ProblemFieldError("originalLocale", "UnsupportedLocale",
                                "Unsupported locale: " + raw))))
                .code();
    }

    private void applyProfileFields(SupplierProfile profile, DraftUpdate update, UUID userId) {
        profile.setLegalName(update.legalName());
        profile.setRegisteredLegalNameTranslated(update.registeredLegalNameTranslated());
        profile.applyTradingName(update.tradingName());
        profile.setRegistrationNumber(update.registrationNumber());
        profile.setCountryOfRegistration(update.countryOfRegistration());
        profile.setRegistrationAuthority(update.registrationAuthority());
        profile.setRegistrationDate(update.registrationDate());
        if (update.companyTypeCode() != null) {
            profile.setCompanyTypeCode(update.companyTypeCode());
        }
        profile.setYearEstablished(update.yearEstablished());
        profile.setEmployeeRange(update.employeeRange());
        profile.setWebsite(update.website());
        profile.setBusinessEmail(update.businessEmail());
        profile.setBusinessPhone(update.businessPhone());
        profile.setUpdatedBy(userId);
    }

    private void applyTypes(UUID supplierId, List<String> types, UUID userId) {
        if (types == null) {
            return;
        }
        types.forEach(referenceCatalog::requireSupplierType);
        typeAssignments.deleteBySupplierId(supplierId);
        for (String code : List.copyOf(new java.util.LinkedHashSet<>(types))) {
            SupplierTypeAssignment assignment = new SupplierTypeAssignment(UuidV7.generate(), supplierId, code);
            assignment.setCreatedBy(userId);
            assignment.setUpdatedBy(userId);
            typeAssignments.save(assignment);
        }
    }

    private void applyCapabilities(UUID supplierId, List<String> capabilities, UUID userId) {
        if (capabilities == null) {
            return;
        }
        capabilities.forEach(referenceCatalog::requireSupplierCapability);
        capabilityDeclarations.deleteBySupplierId(supplierId);
        for (String code : List.copyOf(new java.util.LinkedHashSet<>(capabilities))) {
            SupplierCapabilityDeclaration declaration =
                    new SupplierCapabilityDeclaration(UuidV7.generate(), supplierId, code);
            declaration.setCreatedBy(userId);
            declaration.setUpdatedBy(userId);
            capabilityDeclarations.save(declaration);
        }
    }

    private void requireComplete(Supplier supplier) {
        SupplierProfile profile = profiles.findBySupplierId(supplier.getId()).orElseThrow();
        List<ProblemFieldError> errors = new ArrayList<>();
        if (isBlank(profile.getLegalName())) {
            errors.add(new ProblemFieldError("legalName", "Required", "Legal name is required."));
        }
        if (isBlank(profile.getRegistrationNumber())) {
            errors.add(new ProblemFieldError("registrationNumber", "Required",
                    "Registration number is required."));
        }
        if (isBlank(profile.getCountryOfRegistration())) {
            errors.add(new ProblemFieldError("countryOfRegistration", "Required",
                    "Country of registration is required."));
        }
        if (typeAssignments.findBySupplierIdOrderByTypeCodeAsc(supplier.getId()).isEmpty()) {
            errors.add(new ProblemFieldError("types", "Required", "At least one supplier type is required."));
        }
        if (capabilityDeclarations.findBySupplierIdOrderByCapabilityCodeAsc(supplier.getId()).isEmpty()) {
            errors.add(new ProblemFieldError("capabilities", "Required",
                    "At least one declared capability is required."));
        }
        if (!errors.isEmpty()) {
            throw ApiProblemException.validation(errors);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private SupplierApplicationDto toDto(SupplierApplication application, Supplier supplier,
            com.spexcrafters.organizations.api.OrgRole role) {
        return new SupplierApplicationDto(
                application.getId(),
                supplier.getId(),
                supplier.getOrganizationId(),
                supplier.getOriginalLocale(),
                application.getStatus(),
                supplier.getOperationalStatus(),
                application.getSubmittedAt(),
                application.getDecidedAt(),
                application.getCreatedAt(),
                application.getVersion(),
                SupplierCapability.forRole(role).stream().sorted().toList());
    }

    /**
     * Partial draft update: {@code null} leaves a field unchanged. Blanking a nullable field
     * is expressed by an empty string (normalized to {@code null} by the profile setters).
     * Kept as a nested record to keep the controller request mapping thin.
     */
    public record DraftUpdate(
            String legalName,
            String registeredLegalNameTranslated,
            String tradingName,
            String registrationNumber,
            String countryOfRegistration,
            String registrationAuthority,
            LocalDate registrationDate,
            String companyTypeCode,
            Integer yearEstablished,
            String employeeRange,
            String website,
            String businessEmail,
            String businessPhone,
            List<String> types,
            List<String> capabilities) {
    }

    Optional<SupplierApplication> findApplicationBySupplier(UUID supplierId) {
        return applications.findBySupplierId(supplierId);
    }
}
