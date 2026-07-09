package com.spexcrafters.taxonomy.api;

import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.platformaccess.api.PlatformAccess;
import com.spexcrafters.platformaccess.api.PlatformCapability;
import com.spexcrafters.sharedkernel.i18n.SupportedLocale;
import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import com.spexcrafters.sharedkernel.problem.ProblemFieldError;
import com.spexcrafters.sharedkernel.util.UuidV7;
import com.spexcrafters.taxonomy.domain.TaxonomyConflictException;
import com.spexcrafters.taxonomy.domain.TranslationSource;
import com.spexcrafters.taxonomy.domain.UnitFamily;
import com.spexcrafters.taxonomy.domain.UnitOfMeasure;
import com.spexcrafters.taxonomy.domain.UnitTranslation;
import com.spexcrafters.taxonomy.infrastructure.UnitOfMeasureRepository;
import com.spexcrafters.taxonomy.infrastructure.UnitTranslationRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Unit-of-measure registry reads and administration. */
@Service
public class UnitService {

    private final UnitOfMeasureRepository units;
    private final UnitTranslationRepository translations;
    private final PlatformAccess platformAccess;
    private final AuditLogger audit;

    public UnitService(UnitOfMeasureRepository units, UnitTranslationRepository translations,
            PlatformAccess platformAccess, AuditLogger audit) {
        this.units = units;
        this.translations = translations;
        this.platformAccess = platformAccess;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Unit> list(String locale) {
        Map<String, List<UnitTranslation>> byUnit = translations.findAll().stream()
                .collect(Collectors.groupingBy(UnitTranslation::getUnitCode));
        return units.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(u -> toDto(u, byUnit.getOrDefault(u.getCode(), List.of()), locale))
                .toList();
    }

    /**
     * Administration unit list: platform-staff-only (TAXONOMY_READ). Returns EVERY unit
     * (including inactive) so staff can review all statuses.
     */
    @Transactional(readOnly = true)
    public List<Unit> listForAdmin(UUID userId, String locale) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_READ);
        Map<String, List<UnitTranslation>> byUnit = translations.findAll().stream()
                .collect(Collectors.groupingBy(UnitTranslation::getUnitCode));
        return units.findAllByOrderBySortOrderAsc().stream()
                .map(u -> toDto(u, byUnit.getOrDefault(u.getCode(), List.of()), locale))
                .toList();
    }

    @Transactional
    public Unit create(UUID userId, String code, UnitFamily family, String baseUnitCode, BigDecimal factorToBase,
            BigDecimal offsetToBase, String displayFormat, String originalLocaleRaw, String displayName) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_WRITE);
        if (units.existsById(code)) {
            throw new TaxonomyConflictException("A unit with code " + code + " already exists.");
        }
        if (baseUnitCode != null && !baseUnitCode.isBlank() && !units.existsById(baseUnitCode)) {
            throw ApiProblemException.validation(List.of(new ProblemFieldError(
                    "baseUnitCode", "unknown-reference", "Unknown base unit: " + baseUnitCode)));
        }
        String originalLocale = requireLocale(originalLocaleRaw, "originalLocale");
        UnitOfMeasure unit = new UnitOfMeasure(code, family,
                baseUnitCode == null || baseUnitCode.isBlank() ? null : baseUnitCode, factorToBase,
                offsetToBase == null ? BigDecimal.ZERO : offsetToBase, displayFormat, 0);
        unit.setCreatedBy(userId);
        unit.setUpdatedBy(userId);
        units.save(unit);

        UnitTranslation original = new UnitTranslation(UuidV7.generate(), code, originalLocale, originalLocale,
                1, TranslationSource.HUMAN, true, userId);
        original.applyContent(displayName, 1, TranslationSource.HUMAN, userId);
        original.setCreatedBy(userId);
        original.setUpdatedBy(userId);
        translations.save(original);

        audit.record("taxonomy.unit.created", userId, "unit_of_measure", code, Map.of("code", code));
        return toDto(unit, List.of(original), originalLocale);
    }

    private Unit toDto(UnitOfMeasure unit, List<UnitTranslation> rows, String locale) {
        int currentVersion = rows.stream().filter(UnitTranslation::isOriginal)
                .mapToInt(UnitTranslation::getSourceVersion).findFirst().orElse(1);
        var resolved = LocalizationResolver.resolve(rows, locale, currentVersion);
        String displayName = resolved.isPresent() && resolved.translation().getDisplayName() != null
                ? resolved.translation().getDisplayName() : unit.getCode();
        return new Unit(unit.getCode(), unit.getFamily(), unit.getBaseUnitCode(), unit.getFactorToBase(),
                unit.getOffsetToBase(), displayName, unit.getDisplayFormat());
    }

    private String requireLocale(String raw, String field) {
        return SupportedLocale.parse(raw).map(SupportedLocale::code).orElseThrow(() ->
                ApiProblemException.validation(List.of(new ProblemFieldError(
                        field, "unsupported-locale", "Unsupported locale: " + raw))));
    }
}
