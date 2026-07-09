package com.spexcrafters.taxonomy.api;

import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.platformaccess.api.PlatformAccess;
import com.spexcrafters.platformaccess.api.PlatformCapability;
import com.spexcrafters.sharedkernel.i18n.SupportedLocale;
import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import com.spexcrafters.sharedkernel.problem.ProblemFieldError;
import com.spexcrafters.sharedkernel.util.UuidV7;
import com.spexcrafters.taxonomy.domain.Brand;
import com.spexcrafters.taxonomy.domain.BrandAlias;
import com.spexcrafters.taxonomy.domain.BrandApprovalStatus;
import com.spexcrafters.taxonomy.domain.BrandTranslation;
import com.spexcrafters.taxonomy.domain.BrandType;
import com.spexcrafters.taxonomy.domain.TaxonomyConflictException;
import com.spexcrafters.taxonomy.domain.TaxonomyNotFoundException;
import com.spexcrafters.taxonomy.domain.TranslationSource;
import com.spexcrafters.taxonomy.infrastructure.BrandAliasRepository;
import com.spexcrafters.taxonomy.infrastructure.BrandRepository;
import com.spexcrafters.taxonomy.infrastructure.BrandTranslationRepository;
import com.spexcrafters.taxonomy.infrastructure.CountryRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Brand registry reads (approved-only public) and administration (create/approve/translate). */
@Service
public class BrandService {

    private final BrandRepository brands;
    private final BrandTranslationRepository translations;
    private final BrandAliasRepository aliases;
    private final CountryRepository countries;
    private final PlatformAccess platformAccess;
    private final AuditLogger audit;

    public BrandService(BrandRepository brands, BrandTranslationRepository translations,
            BrandAliasRepository aliases, CountryRepository countries, PlatformAccess platformAccess,
            AuditLogger audit) {
        this.brands = brands;
        this.translations = translations;
        this.aliases = aliases;
        this.countries = countries;
        this.platformAccess = platformAccess;
        this.audit = audit;
    }

    // ------------------------------------------------------------------ public reads

    @Transactional(readOnly = true)
    public List<BrandSummary> listPublic(String locale) {
        List<Brand> approved = brands.findByApprovalStatusOrderByCanonicalNameAsc(BrandApprovalStatus.APPROVED);
        List<UUID> ids = approved.stream().map(Brand::getId).toList();
        Map<UUID, List<BrandTranslation>> byBrand = ids.isEmpty() ? Map.of()
                : translations.findByBrandIdIn(ids).stream()
                        .collect(Collectors.groupingBy(BrandTranslation::getBrandId));
        return approved.stream()
                .map(b -> new BrandSummary(b.getId(), b.getCode(), b.getBrandType(), b.getCanonicalName(),
                        resolveDisplayName(b, byBrand.getOrDefault(b.getId(), List.of()), locale),
                        b.getCountryCode(), b.getApprovalStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public BrandDetail getPublic(String code, String locale) {
        Brand brand = brands.findByCode(code)
                .filter(b -> b.getApprovalStatus() == BrandApprovalStatus.APPROVED)
                .orElseThrow(TaxonomyNotFoundException::new);
        return toDetail(brand, locale);
    }

    // ------------------------------------------------------------------ administration

    /**
     * Administration brand list: platform-staff-only, returns brands of EVERY approval status
     * (including PENDING/REJECTED/DEPRECATED) so staff can review and approve them. Doubles as the
     * read-time staff gate for the admin dashboard (public reads never surface a 403).
     */
    @Transactional(readOnly = true)
    public List<BrandSummary> listForAdmin(UUID userId, String locale) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_READ);
        List<Brand> all = brands.findAllByOrderByCanonicalNameAsc();
        List<UUID> ids = all.stream().map(Brand::getId).toList();
        Map<UUID, List<BrandTranslation>> byBrand = ids.isEmpty() ? Map.of()
                : translations.findByBrandIdIn(ids).stream()
                        .collect(Collectors.groupingBy(BrandTranslation::getBrandId));
        return all.stream()
                .map(b -> new BrandSummary(b.getId(), b.getCode(), b.getBrandType(), b.getCanonicalName(),
                        resolveDisplayName(b, byBrand.getOrDefault(b.getId(), List.of()), locale),
                        b.getCountryCode(), b.getApprovalStatus()))
                .toList();
    }

    @Transactional
    public BrandDetail create(UUID userId, String code, BrandType brandType, String canonicalName,
            String ownerCompany, String manufacturer, String countryCode, String website,
            String originalLocaleRaw, String displayName) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_WRITE);
        if (brands.existsByCode(code)) {
            throw new TaxonomyConflictException("A brand with code " + code + " already exists.");
        }
        if (countryCode != null && !countryCode.isBlank() && !countries.existsById(countryCode)) {
            throw ApiProblemException.validation(List.of(new ProblemFieldError(
                    "countryCode", "unknown-reference", "Unknown country: " + countryCode)));
        }
        String originalLocale = requireLocale(originalLocaleRaw, "originalLocale");
        Brand brand = new Brand(UuidV7.generate(), code, brandType, canonicalName, ownerCompany, manufacturer,
                countryCode == null || countryCode.isBlank() ? null : countryCode, website);
        brand.setCreatedBy(userId);
        brand.setUpdatedBy(userId);
        brands.save(brand);

        BrandTranslation original = new BrandTranslation(UuidV7.generate(), brand.getId(), originalLocale,
                originalLocale, brand.getSourceVersion(), TranslationSource.HUMAN, true, userId);
        original.applyContent(displayName, null, brand.getSourceVersion(), TranslationSource.HUMAN, userId);
        original.setCreatedBy(userId);
        original.setUpdatedBy(userId);
        translations.save(original);

        audit.record("taxonomy.brand.created", userId, "brand", brand.getId().toString(),
                Map.of("code", code));
        return toDetail(brand, originalLocale);
    }

    @Transactional
    public BrandDetail setApproval(UUID userId, UUID id, BrandApprovalStatus status) {
        platformAccess.require(userId, PlatformCapability.BRAND_APPROVE);
        Brand brand = brands.findById(id).orElseThrow(TaxonomyNotFoundException::new);
        brand.setApprovalStatus(status);
        brand.setUpdatedBy(userId);
        audit.record("taxonomy.brand.approval." + status.name().toLowerCase(java.util.Locale.ROOT),
                userId, "brand", id.toString(), Map.of("code", brand.getCode(), "status", status.name()));
        return toDetail(brand, brand.getCode());
    }

    @Transactional
    public TranslationView upsertTranslation(UUID userId, UUID id, String localeRaw, String displayName,
            String description, TranslationSource sourceRaw) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_WRITE);
        Brand brand = brands.findById(id).orElseThrow(TaxonomyNotFoundException::new);
        String locale = requireLocale(localeRaw, "locale");
        List<BrandTranslation> rows = translations.findByBrandId(id);
        boolean isOriginal = rows.stream().anyMatch(t -> t.isOriginal() && t.getLocale().equals(locale));
        TranslationSource source = sourceRaw == null ? TranslationSource.HUMAN : sourceRaw;
        if (isOriginal && source == TranslationSource.MACHINE) {
            throw ApiProblemException.validation(List.of(new ProblemFieldError(
                    "source", "invalid-source", "Original-language content cannot be machine-translated.")));
        }
        int targetVersion = isOriginal ? brand.bumpSourceVersion() : brand.getSourceVersion();
        String originalLocale = rows.stream().filter(BrandTranslation::isOriginal)
                .map(BrandTranslation::getLocale).findFirst().orElse(SupportedLocale.FALLBACK.code());
        Optional<BrandTranslation> existing = rows.stream()
                .filter(t -> t.getLocale().equals(locale)).findFirst();
        BrandTranslation translation = existing.orElseGet(() -> {
            BrandTranslation created = new BrandTranslation(UuidV7.generate(), id, locale, originalLocale,
                    targetVersion, source, false, userId);
            created.setCreatedBy(userId);
            return created;
        });
        translation.applyContent(displayName, description, targetVersion, source, userId);
        translation.setUpdatedBy(userId);
        translations.save(translation);
        brand.setUpdatedBy(userId);
        audit.record("taxonomy.brand.translation.upserted", userId, "brand_translation",
                translation.getId().toString(), Map.of("brandId", id.toString(), "locale", locale));
        return new TranslationView(translation.getLocale(), translation.getDisplayName(),
                translation.getDescription(), translation.getTranslationStatus(), translation.isOriginal(),
                translation.isStale(brand.getSourceVersion()), translation.getSourceVersion());
    }

    private BrandDetail toDetail(Brand brand, String locale) {
        List<BrandTranslation> rows = translations.findByBrandId(brand.getId());
        String displayName = resolveDisplayName(brand, rows, locale);
        List<String> aliasList = aliases.findByBrandIdOrderByAliasAsc(brand.getId()).stream()
                .map(BrandAlias::getAlias).toList();
        return new BrandDetail(brand.getId(), brand.getCode(), brand.getBrandType(), brand.getCanonicalName(),
                displayName, brand.getOwnerCompany(), brand.getManufacturer(), brand.getCountryCode(),
                brand.getWebsite(), brand.getLogoStorageKey(), brand.getApprovalStatus(), aliasList,
                brand.getVersion());
    }

    private String resolveDisplayName(Brand brand, List<BrandTranslation> rows, String locale) {
        var resolved = LocalizationResolver.resolve(rows, locale, brand.getSourceVersion());
        return resolved.isPresent() ? resolved.translation().getDisplayName() : null;
    }

    private String requireLocale(String raw, String field) {
        return SupportedLocale.parse(raw).map(SupportedLocale::code).orElseThrow(() ->
                ApiProblemException.validation(List.of(new ProblemFieldError(
                        field, "unsupported-locale", "Unsupported locale: " + raw))));
    }
}
