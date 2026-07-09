package com.spexcrafters.taxonomy.api;

import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.platformaccess.api.PlatformAccess;
import com.spexcrafters.platformaccess.api.PlatformCapability;
import com.spexcrafters.sharedkernel.i18n.SupportedLocale;
import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import com.spexcrafters.sharedkernel.problem.ProblemFieldError;
import com.spexcrafters.sharedkernel.util.UuidV7;
import com.spexcrafters.taxonomy.domain.CertificationCategory;
import com.spexcrafters.taxonomy.domain.CertificationTranslation;
import com.spexcrafters.taxonomy.domain.TaxonomyConflictException;
import com.spexcrafters.taxonomy.domain.TranslationSource;
import com.spexcrafters.taxonomy.infrastructure.CertificationRepository;
import com.spexcrafters.taxonomy.infrastructure.CertificationTranslationRepository;
import com.spexcrafters.taxonomy.infrastructure.CountryRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Certification registry reads and administration. */
@Service
public class CertificationService {

    private final CertificationRepository certifications;
    private final CertificationTranslationRepository translations;
    private final CountryRepository countries;
    private final PlatformAccess platformAccess;
    private final AuditLogger audit;

    public CertificationService(CertificationRepository certifications,
            CertificationTranslationRepository translations, CountryRepository countries,
            PlatformAccess platformAccess, AuditLogger audit) {
        this.certifications = certifications;
        this.translations = translations;
        this.countries = countries;
        this.platformAccess = platformAccess;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Certification> list(String locale) {
        // Public read: inactive and deprecated certifications are excluded.
        Map<UUID, List<CertificationTranslation>> byCert = translations.findAll().stream()
                .collect(Collectors.groupingBy(CertificationTranslation::getCertificationId));
        return certifications.findAllByOrderBySortOrderAsc().stream()
                .filter(c -> c.isActive() && !c.isDeprecated())
                .map(c -> toDto(c, byCert.getOrDefault(c.getId(), List.of()), locale))
                .toList();
    }

    /**
     * Administration certification list: platform-staff-only (TAXONOMY_READ). Returns EVERY
     * certification (including deprecated and inactive) so staff can review all statuses.
     */
    @Transactional(readOnly = true)
    public List<Certification> listForAdmin(UUID userId, String locale) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_READ);
        Map<UUID, List<CertificationTranslation>> byCert = translations.findAll().stream()
                .collect(Collectors.groupingBy(CertificationTranslation::getCertificationId));
        return certifications.findAllByOrderBySortOrderAsc().stream()
                .map(c -> toDto(c, byCert.getOrDefault(c.getId(), List.of()), locale))
                .toList();
    }

    @Transactional
    public Certification create(UUID userId, String code, CertificationCategory category, String countryScope,
            String industryScope, Integer validityMonths, String originalLocaleRaw, String name,
            String description) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_WRITE);
        if (certifications.existsByCode(code)) {
            throw new TaxonomyConflictException("A certification with code " + code + " already exists.");
        }
        if (countryScope != null && !countryScope.isBlank() && !countries.existsById(countryScope)) {
            throw ApiProblemException.validation(List.of(new ProblemFieldError(
                    "countryScope", "unknown-reference", "Unknown country: " + countryScope)));
        }
        String originalLocale = requireLocale(originalLocaleRaw, "originalLocale");
        com.spexcrafters.taxonomy.domain.Certification cert =
                new com.spexcrafters.taxonomy.domain.Certification(UuidV7.generate(), code, category,
                        countryScope == null || countryScope.isBlank() ? null : countryScope, industryScope,
                        validityMonths, 0);
        cert.setCreatedBy(userId);
        cert.setUpdatedBy(userId);
        certifications.save(cert);

        CertificationTranslation original = new CertificationTranslation(UuidV7.generate(), cert.getId(),
                originalLocale, originalLocale, 1, TranslationSource.HUMAN, true, userId);
        original.applyContent(name, description, 1, TranslationSource.HUMAN, userId);
        original.setCreatedBy(userId);
        original.setUpdatedBy(userId);
        translations.save(original);

        audit.record("taxonomy.certification.created", userId, "certification", cert.getId().toString(),
                Map.of("code", code));
        return toDto(cert, List.of(original), originalLocale);
    }

    private Certification toDto(com.spexcrafters.taxonomy.domain.Certification cert,
            List<CertificationTranslation> rows, String locale) {
        int currentVersion = rows.stream().filter(CertificationTranslation::isOriginal)
                .mapToInt(CertificationTranslation::getSourceVersion).findFirst().orElse(1);
        var resolved = LocalizationResolver.resolve(rows, locale, currentVersion);
        String name = resolved.isPresent() && resolved.translation().getName() != null
                ? resolved.translation().getName() : cert.getCode();
        String description = resolved.isPresent() ? resolved.translation().getDescription() : null;
        return new Certification(cert.getId(), cert.getCode(), cert.getCategory(), cert.getCountryScope(),
                cert.getIndustryScope(), cert.getValidityMonths(), name, description, cert.isDeprecated());
    }

    private String requireLocale(String raw, String field) {
        return SupportedLocale.parse(raw).map(SupportedLocale::code).orElseThrow(() ->
                ApiProblemException.validation(List.of(new ProblemFieldError(
                        field, "unsupported-locale", "Unsupported locale: " + raw))));
    }
}
