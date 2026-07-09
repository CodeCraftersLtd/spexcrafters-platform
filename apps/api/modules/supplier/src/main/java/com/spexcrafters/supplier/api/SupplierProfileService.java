package com.spexcrafters.supplier.api;

import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.sharedkernel.i18n.SupportedLocale;
import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import com.spexcrafters.sharedkernel.problem.ProblemFieldError;
import com.spexcrafters.sharedkernel.util.UuidV7;
import com.spexcrafters.supplier.domain.AddressPrivacy;
import com.spexcrafters.supplier.domain.FacilityOwnership;
import com.spexcrafters.supplier.domain.Supplier;
import com.spexcrafters.supplier.domain.SupplierFacility;
import com.spexcrafters.supplier.domain.SupplierFacilityTranslation;
import com.spexcrafters.supplier.domain.SupplierNotFoundException;
import com.spexcrafters.supplier.domain.SupplierProfile;
import com.spexcrafters.supplier.domain.SupplierProfileTranslation;
import com.spexcrafters.supplier.domain.TranslationSource;
import com.spexcrafters.supplier.infrastructure.SupplierCapabilityDeclarationRepository;
import com.spexcrafters.supplier.infrastructure.SupplierFacilityRepository;
import com.spexcrafters.supplier.infrastructure.SupplierFacilityTranslationRepository;
import com.spexcrafters.supplier.infrastructure.SupplierProfileRepository;
import com.spexcrafters.supplier.infrastructure.SupplierProfileTranslationRepository;
import com.spexcrafters.supplier.infrastructure.SupplierTypeAssignmentRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supplier profile reads and translation/facility management (ADR-020). Preserves the
 * original-language content (never overwritten by a translation), records the source version
 * each translation targets and marks translations stale when the source content changes.
 */
@Service
public class SupplierProfileService {

    private final SupplierProfileRepository profiles;
    private final SupplierProfileTranslationRepository profileTranslations;
    private final SupplierTypeAssignmentRepository typeAssignments;
    private final SupplierCapabilityDeclarationRepository capabilityDeclarations;
    private final SupplierFacilityRepository facilities;
    private final SupplierFacilityTranslationRepository facilityTranslations;
    private final SupplierAccess access;
    private final ReferenceCatalog referenceCatalog;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public SupplierProfileService(SupplierProfileRepository profiles,
            SupplierProfileTranslationRepository profileTranslations,
            SupplierTypeAssignmentRepository typeAssignments,
            SupplierCapabilityDeclarationRepository capabilityDeclarations,
            SupplierFacilityRepository facilities,
            SupplierFacilityTranslationRepository facilityTranslations,
            SupplierAccess access,
            ReferenceCatalog referenceCatalog,
            AuditLogger auditLogger,
            Clock clock) {
        this.profiles = profiles;
        this.profileTranslations = profileTranslations;
        this.typeAssignments = typeAssignments;
        this.capabilityDeclarations = capabilityDeclarations;
        this.facilities = facilities;
        this.facilityTranslations = facilityTranslations;
        this.access = access;
        this.referenceCatalog = referenceCatalog;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SupplierProfileDto getProfile(UUID userId, UUID supplierId) {
        SupplierContext context = access.requireForSupplier(userId, supplierId, SupplierCapability.READ);
        return buildProfileDto(context.supplier());
    }

    /**
     * Creates or updates the translation for {@code locale}. Editing the original-language row
     * bumps the profile source version (marking other-language translations stale); editing a
     * non-original row records the current source version and resets its approval.
     */
    @Transactional
    public ProfileTranslationDto upsertTranslation(UUID userId, UUID supplierId, String rawLocale,
            TranslationContent content) {
        SupplierContext context = access.requireForSupplier(userId, supplierId, SupplierCapability.UPDATE);
        String locale = requireLocale(rawLocale);
        SupplierProfile profile = loadProfile(supplierId);
        boolean isOriginal = locale.equals(context.supplier().getOriginalLocale());
        TranslationSource source = content.source() == null ? TranslationSource.HUMAN : content.source();
        if (isOriginal && source == TranslationSource.MACHINE) {
            throw ApiProblemException.validation(List.of(new ProblemFieldError(
                    "source", "InvalidSource", "Original-language content cannot be machine-translated.")));
        }

        Optional<SupplierProfileTranslation> existing =
                profileTranslations.findByProfileIdAndLocale(profile.getId(), locale);
        int targetVersion = isOriginal ? profile.bumpSourceVersion() : profile.getSourceVersion();

        SupplierProfileTranslation translation = existing.orElseGet(() -> {
            SupplierProfileTranslation created = new SupplierProfileTranslation(
                    UuidV7.generate(), profile.getId(), locale, context.supplier().getOriginalLocale(),
                    targetVersion, source, isOriginal, userId);
            created.setCreatedBy(userId);
            return created;
        });
        translation.applyContent(content.tradingName(), content.companyDescription(),
                content.productionCapabilityDescription(), content.oemDescription(), content.odmDescription(),
                content.privateLabelDescription(), content.qualityControlDescription(),
                content.exportMarketDescription(), targetVersion, source, userId);
        translation.setUpdatedBy(userId);
        profileTranslations.save(translation);
        profile.setUpdatedBy(userId);

        auditLogger.record(existing.isPresent() ? "supplier.translation.updated"
                        : "supplier.translation.created",
                userId, "supplier_profile_translation", translation.getId().toString(),
                Map.of("supplierId", supplierId.toString(), "locale", locale));
        return toDto(translation, profile.getSourceVersion());
    }

    @Transactional
    public ProfileTranslationDto approveTranslation(UUID userId, UUID supplierId, String rawLocale) {
        return decideTranslation(userId, supplierId, rawLocale, true);
    }

    @Transactional
    public ProfileTranslationDto rejectTranslation(UUID userId, UUID supplierId, String rawLocale) {
        return decideTranslation(userId, supplierId, rawLocale, false);
    }

    private ProfileTranslationDto decideTranslation(UUID userId, UUID supplierId, String rawLocale,
            boolean approve) {
        SupplierContext context = access.requireForSupplier(userId, supplierId, SupplierCapability.UPDATE);
        String locale = requireLocale(rawLocale);
        SupplierProfile profile = loadProfile(supplierId);
        SupplierProfileTranslation translation = profileTranslations
                .findByProfileIdAndLocale(profile.getId(), locale)
                .orElseThrow(SupplierNotFoundException::new);
        if (translation.isOriginal()) {
            throw ApiProblemException.validation(List.of(new ProblemFieldError(
                    "locale", "OriginalLocale", "The original-language content is authoritative.")));
        }
        if (approve) {
            translation.approve(userId, clock.instant());
        } else {
            translation.reject(userId);
        }
        translation.setUpdatedBy(userId);
        auditLogger.record(approve ? "supplier.translation.approved" : "supplier.translation.rejected",
                userId, "supplier_profile_translation", translation.getId().toString(),
                Map.of("supplierId", supplierId.toString(), "locale", locale));
        return toDto(translation, profile.getSourceVersion());
    }

    @Transactional
    public FacilityDto addFacility(UUID userId, UUID supplierId, String facilityTypeCode, String country,
            String region, String city, AddressPrivacy addressPrivacy, FacilityOwnership ownership,
            boolean isPublic, String name, String description) {
        SupplierContext context = access.requireForSupplier(userId, supplierId, SupplierCapability.UPDATE);
        referenceCatalog.requireFacilityType(facilityTypeCode);
        SupplierFacility facility = new SupplierFacility(UuidV7.generate(), supplierId, facilityTypeCode,
                country, addressPrivacy, ownership, isPublic);
        facility.setRegion(region);
        facility.setCity(city);
        facility.setCreatedBy(userId);
        facility.setUpdatedBy(userId);
        facilities.save(facility);

        SupplierFacilityTranslation original = new SupplierFacilityTranslation(
                UuidV7.generate(), facility.getId(), context.supplier().getOriginalLocale(),
                facility.getSourceVersion(), true);
        original.applyContent(name, description, facility.getSourceVersion());
        original.setCreatedBy(userId);
        original.setUpdatedBy(userId);
        facilityTranslations.save(original);
        return toFacilityDto(facility);
    }

    // ---------------------------------------------------------------- mapping

    /**
     * Builds the full profile view for a caller already authorized out-of-band (the reviewer
     * workflow, authorized by a platform capability rather than an org capability).
     */
    public SupplierProfileDto buildProfileDto(Supplier supplier) {
        SupplierProfile profile = loadProfile(supplier.getId());
        List<String> types = typeAssignments.findBySupplierIdOrderByTypeCodeAsc(supplier.getId()).stream()
                .map(com.spexcrafters.supplier.domain.SupplierTypeAssignment::getTypeCode).toList();
        List<CapabilityDeclarationDto> caps = capabilityDeclarations
                .findBySupplierIdOrderByCapabilityCodeAsc(supplier.getId()).stream()
                .map(d -> new CapabilityDeclarationDto(d.getCapabilityCode(), d.getClaimStatus())).toList();
        List<ProfileTranslationDto> translations = profileTranslations
                .findByProfileIdOrderByLocaleAsc(profile.getId()).stream()
                .map(t -> toDto(t, profile.getSourceVersion())).toList();
        List<FacilityDto> facilityDtos = facilities.findBySupplierIdOrderByCreatedAtAsc(supplier.getId())
                .stream().map(this::toFacilityDto).toList();
        return new SupplierProfileDto(
                supplier.getId(),
                supplier.getOriginalLocale(),
                profile.getLegalName(),
                profile.getRegisteredLegalNameTranslated(),
                profile.getTradingName(),
                profile.getRegistrationNumber(),
                profile.getCountryOfRegistration(),
                profile.getRegistrationAuthority(),
                profile.getRegistrationDate(),
                profile.getCompanyTypeCode(),
                profile.getYearEstablished(),
                profile.getEmployeeRange(),
                profile.getWebsite(),
                profile.getBusinessEmail(),
                profile.getBusinessPhone(),
                profile.getSourceVersion(),
                types,
                caps,
                facilityDtos,
                translations);
    }

    private static ProfileTranslationDto toDto(SupplierProfileTranslation t, int currentSourceVersion) {
        return new ProfileTranslationDto(
                t.getLocale(), t.isOriginal(), t.getTranslationStatus(), t.getTranslationSource(),
                t.getSourceLocale(), t.getSourceVersion(), t.isStale(currentSourceVersion),
                t.getTradingName(), t.getCompanyDescription(), t.getProductionCapabilityDescription(),
                t.getOemDescription(), t.getOdmDescription(), t.getPrivateLabelDescription(),
                t.getQualityControlDescription(), t.getExportMarketDescription());
    }

    private FacilityDto toFacilityDto(SupplierFacility facility) {
        List<FacilityTranslationDto> translations = facilityTranslations
                .findByFacilityIdOrderByLocaleAsc(facility.getId()).stream()
                .map(t -> new FacilityTranslationDto(t.getLocale(), t.isOriginal(), t.getTranslationStatus(),
                        t.getSourceVersion(), t.isStale(facility.getSourceVersion()), t.getName(),
                        t.getDescription()))
                .toList();
        return new FacilityDto(facility.getId(), facility.getFacilityTypeCode(), facility.getCountry(),
                facility.getRegion(), facility.getCity(), facility.getAddressPrivacy(), facility.getOwnership(),
                facility.isPublic(), facility.getSourceVersion(), translations);
    }

    private SupplierProfile loadProfile(UUID supplierId) {
        return profiles.findBySupplierId(supplierId).orElseThrow(SupplierNotFoundException::new);
    }

    private String requireLocale(String rawLocale) {
        return SupportedLocale.parse(rawLocale)
                .orElseThrow(() -> ApiProblemException.validation(List.of(
                        new ProblemFieldError("locale", "UnsupportedLocale", "Unsupported locale: " + rawLocale))))
                .code();
    }
}
