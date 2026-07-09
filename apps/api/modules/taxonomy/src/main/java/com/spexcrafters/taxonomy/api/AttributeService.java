package com.spexcrafters.taxonomy.api;

import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.platformaccess.api.PlatformAccess;
import com.spexcrafters.platformaccess.api.PlatformCapability;
import com.spexcrafters.sharedkernel.i18n.SupportedLocale;
import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import com.spexcrafters.sharedkernel.problem.ProblemFieldError;
import com.spexcrafters.sharedkernel.util.UuidV7;
import com.spexcrafters.taxonomy.domain.Attribute;
import com.spexcrafters.taxonomy.domain.AttributeDataType;
import com.spexcrafters.taxonomy.domain.AttributeTranslation;
import com.spexcrafters.taxonomy.domain.Enumeration;
import com.spexcrafters.taxonomy.domain.TaxonomyConflictException;
import com.spexcrafters.taxonomy.domain.TaxonomyNotFoundException;
import com.spexcrafters.taxonomy.domain.TranslationSource;
import com.spexcrafters.taxonomy.infrastructure.AttributeRepository;
import com.spexcrafters.taxonomy.infrastructure.AttributeTranslationRepository;
import com.spexcrafters.taxonomy.infrastructure.EnumerationRepository;
import com.spexcrafters.taxonomy.infrastructure.UnitOfMeasureRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Attribute registry reads and administration (create/update/deprecate/translate). */
@Service
public class AttributeService {

    private static final Set<AttributeDataType> UNIT_TYPES =
            EnumSet.of(AttributeDataType.MEASUREMENT, AttributeDataType.RANGE);
    private static final Set<AttributeDataType> ENUM_TYPES =
            EnumSet.of(AttributeDataType.ENUMERATION, AttributeDataType.SINGLE_SELECT,
                    AttributeDataType.MULTI_SELECT);

    private final AttributeRepository attributes;
    private final AttributeTranslationRepository translations;
    private final UnitOfMeasureRepository units;
    private final EnumerationRepository enumerations;
    private final PlatformAccess platformAccess;
    private final AuditLogger audit;
    private final Clock clock;

    public AttributeService(AttributeRepository attributes, AttributeTranslationRepository translations,
            UnitOfMeasureRepository units, EnumerationRepository enumerations, PlatformAccess platformAccess,
            AuditLogger audit, Clock clock) {
        this.attributes = attributes;
        this.translations = translations;
        this.units = units;
        this.enumerations = enumerations;
        this.platformAccess = platformAccess;
        this.audit = audit;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ reads

    @Transactional(readOnly = true)
    public List<AttributeSummary> list(String locale) {
        // Public read: deprecated and non-visible attributes are excluded.
        List<Attribute> all = attributes.findAllByOrderBySortOrderAsc().stream()
                .filter(a -> !a.isDeprecated() && a.isVisible())
                .toList();
        List<UUID> ids = all.stream().map(Attribute::getId).toList();
        Map<UUID, List<AttributeTranslation>> byAttr = translations.findByAttributeIdIn(ids).stream()
                .collect(Collectors.groupingBy(AttributeTranslation::getAttributeId));
        Map<UUID, String> enumCodes = enumerationCodes(all);
        List<AttributeSummary> result = new ArrayList<>();
        for (Attribute attr : all) {
            String name = resolveName(attr, byAttr.getOrDefault(attr.getId(), List.of()), locale);
            result.add(new AttributeSummary(attr.getCode(), attr.getDataType(), name, attr.getUnitCode(),
                    attr.getEnumerationId() == null ? null : enumCodes.get(attr.getEnumerationId()),
                    attr.isDeprecated(), attr.isVisible(), attr.isSearchable(), attr.isFilterable(),
                    attr.isSortable(), attr.isComparable(), attr.isFacetable(), attr.isSeo()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public AttributeDetail get(String code, String locale) {
        // Public read: a deprecated or non-visible attribute is hidden (404).
        Attribute attr = attributes.findByCode(code)
                .filter(a -> !a.isDeprecated() && a.isVisible())
                .orElseThrow(TaxonomyNotFoundException::new);
        return toDetail(attr, locale);
    }

    /**
     * Administration attribute list: platform-staff-only (TAXONOMY_READ). Returns EVERY attribute
     * (including deprecated and non-visible) as full {@link AttributeDetail}s so staff can review
     * and edit them regardless of status.
     */
    @Transactional(readOnly = true)
    public List<AttributeDetail> listForAdmin(UUID userId, String locale) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_READ);
        return attributes.findAllByOrderBySortOrderAsc().stream()
                .map(attr -> toDetail(attr, locale))
                .toList();
    }

    // ------------------------------------------------------------------ administration

    @Transactional
    public AttributeDetail create(UUID userId, CreateAttributeInput in) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_WRITE);
        if (attributes.existsByCode(in.code())) {
            throw new TaxonomyConflictException("An attribute with code " + in.code() + " already exists.");
        }
        String originalLocale = requireLocale(in.originalLocale(), "originalLocale");
        UUID enumerationId = resolveEnumeration(in.enumerationCode());
        validateCrossField(in.dataType(), in.unitCode(), enumerationId, in.regexPattern());

        Attribute attr = new Attribute(UuidV7.generate(), in.code(), in.dataType(), 0);
        attr.setUnitCode(in.unitCode());
        attr.setEnumerationId(enumerationId);
        attr.setBounds(in.minValue(), in.maxValue(), in.minLength(), in.maxLength(), in.regexPattern());
        attr.setCapabilityFlags(in.searchable(), in.filterable(), in.sortable(), in.comparable(),
                in.facetable(), in.seo(), in.visible() == null || in.visible());
        attr.setCreatedBy(userId);
        attr.setUpdatedBy(userId);
        attributes.save(attr);

        AttributeTranslation original = new AttributeTranslation(UuidV7.generate(), attr.getId(), originalLocale,
                originalLocale, attr.getSourceVersion(), TranslationSource.HUMAN, true, userId);
        original.applyContent(in.name(), in.description(), attr.getSourceVersion(), TranslationSource.HUMAN,
                userId);
        original.setCreatedBy(userId);
        original.setUpdatedBy(userId);
        translations.save(original);

        audit.record("taxonomy.attribute.created", userId, "attribute", attr.getId().toString(),
                Map.of("code", in.code()));
        return toDetail(attr, originalLocale);
    }

    @Transactional
    public AttributeDetail update(UUID userId, UUID id, UpdateAttributeInput in, int expectedVersion) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_WRITE);
        Attribute attr = attributes.findById(id).orElseThrow(TaxonomyNotFoundException::new);
        requireVersion(attr, expectedVersion);
        UUID enumerationId = resolveEnumeration(in.enumerationCode());
        validateCrossField(attr.getDataType(), in.unitCode(), enumerationId, in.regexPattern());
        attr.setUnitCode(in.unitCode());
        attr.setEnumerationId(enumerationId);
        attr.setBounds(in.minValue(), in.maxValue(), in.minLength(), in.maxLength(), in.regexPattern());
        attr.setCapabilityFlags(in.searchable(), in.filterable(), in.sortable(), in.comparable(),
                in.facetable(), in.seo(), in.visible() == null || in.visible());
        attr.setUpdatedBy(userId);
        audit.record("taxonomy.attribute.updated", userId, "attribute", id.toString(),
                Map.of("code", attr.getCode()));
        return toDetail(attr, attr.getCode());
    }

    @Transactional
    public AttributeDetail setDeprecation(UUID userId, UUID id, boolean deprecated) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_WRITE);
        Attribute attr = attributes.findById(id).orElseThrow(TaxonomyNotFoundException::new);
        attr.setDeprecated(deprecated);
        attr.setUpdatedBy(userId);
        audit.record(deprecated ? "taxonomy.attribute.deprecated" : "taxonomy.attribute.reinstated",
                userId, "attribute", id.toString(), Map.of("code", attr.getCode()));
        return toDetail(attr, attr.getCode());
    }

    @Transactional
    public TranslationView upsertTranslation(UUID userId, UUID id, String localeRaw, String name,
            String description, TranslationSource sourceRaw) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_WRITE);
        Attribute attr = attributes.findById(id).orElseThrow(TaxonomyNotFoundException::new);
        String locale = requireLocale(localeRaw, "locale");
        List<AttributeTranslation> rows = translations.findByAttributeId(id);
        boolean isOriginal = rows.stream().anyMatch(t -> t.isOriginal() && t.getLocale().equals(locale));
        TranslationSource source = sourceRaw == null ? TranslationSource.HUMAN : sourceRaw;
        if (isOriginal && source == TranslationSource.MACHINE) {
            throw ApiProblemException.validation(List.of(new ProblemFieldError(
                    "source", "invalid-source", "Original-language content cannot be machine-translated.")));
        }
        int targetVersion = isOriginal ? attr.bumpSourceVersion() : attr.getSourceVersion();
        String originalLocale = rows.stream().filter(AttributeTranslation::isOriginal)
                .map(AttributeTranslation::getLocale).findFirst().orElse(SupportedLocale.FALLBACK.code());
        Optional<AttributeTranslation> existing = rows.stream()
                .filter(t -> t.getLocale().equals(locale)).findFirst();
        AttributeTranslation translation = existing.orElseGet(() -> {
            AttributeTranslation created = new AttributeTranslation(UuidV7.generate(), id, locale,
                    originalLocale, targetVersion, source, false, userId);
            created.setCreatedBy(userId);
            return created;
        });
        translation.applyContent(name, description, targetVersion, source, userId);
        translation.setUpdatedBy(userId);
        translations.save(translation);
        attr.setUpdatedBy(userId);
        audit.record("taxonomy.attribute.translation.upserted", userId, "attribute_translation",
                translation.getId().toString(), Map.of("attributeId", id.toString(), "locale", locale));
        return new TranslationView(translation.getLocale(), translation.getName(),
                translation.getDescription(), translation.getTranslationStatus(), translation.isOriginal(),
                translation.isStale(attr.getSourceVersion()), translation.getSourceVersion());
    }

    /** Create request payload (mirrors CreateAttributeRequest). */
    public record CreateAttributeInput(String code, AttributeDataType dataType, String unitCode,
            String enumerationCode, BigDecimal minValue, BigDecimal maxValue, Integer minLength,
            Integer maxLength, String regexPattern, boolean searchable, boolean filterable, boolean sortable,
            boolean comparable, boolean facetable, boolean seo, Boolean visible, String originalLocale,
            String name, String description) {
    }

    /** Update request payload (mirrors UpdateAttributeRequest). */
    public record UpdateAttributeInput(String unitCode, String enumerationCode, BigDecimal minValue,
            BigDecimal maxValue, Integer minLength, Integer maxLength, String regexPattern, boolean searchable,
            boolean filterable, boolean sortable, boolean comparable, boolean facetable, boolean seo,
            Boolean visible) {
    }

    // ------------------------------------------------------------------ helpers

    private void validateCrossField(AttributeDataType dataType, String unitCode, UUID enumerationId,
            String regexPattern) {
        List<ProblemFieldError> errors = new ArrayList<>();
        if (unitCode != null && !UNIT_TYPES.contains(dataType)) {
            errors.add(new ProblemFieldError("unitCode", "invalid-for-type",
                    "A unit is only valid for MEASUREMENT or RANGE attributes."));
        }
        if (unitCode != null && !units.existsById(unitCode)) {
            errors.add(new ProblemFieldError("unitCode", "unknown-reference", "Unknown unit: " + unitCode));
        }
        if (enumerationId != null && !ENUM_TYPES.contains(dataType)) {
            errors.add(new ProblemFieldError("enumerationCode", "invalid-for-type",
                    "An enumeration is only valid for ENUMERATION/SINGLE_SELECT/MULTI_SELECT attributes."));
        }
        if (regexPattern != null && !regexPattern.isBlank()) {
            try {
                Pattern.compile(regexPattern);
            } catch (PatternSyntaxException ex) {
                errors.add(new ProblemFieldError("regexPattern", "invalid-pattern", "Regex does not compile."));
            }
        }
        if (!errors.isEmpty()) {
            throw ApiProblemException.validation(errors);
        }
    }

    private UUID resolveEnumeration(String enumerationCode) {
        if (enumerationCode == null || enumerationCode.isBlank()) {
            return null;
        }
        return enumerations.findByCode(enumerationCode).map(Enumeration::getId).orElseThrow(() ->
                ApiProblemException.validation(List.of(new ProblemFieldError(
                        "enumerationCode", "unknown-reference", "Unknown enumeration: " + enumerationCode))));
    }

    private Map<UUID, String> enumerationCodes(List<Attribute> all) {
        Map<UUID, String> codes = new LinkedHashMap<>();
        for (Enumeration e : enumerations.findAll()) {
            codes.put(e.getId(), e.getCode());
        }
        return codes;
    }

    private AttributeDetail toDetail(Attribute attr, String locale) {
        List<AttributeTranslation> rows = translations.findByAttributeId(attr.getId());
        var resolved = LocalizationResolver.resolve(rows, locale, attr.getSourceVersion());
        String name = resolved.isPresent() && resolved.translation().getName() != null
                ? resolved.translation().getName() : attr.getCode();
        String description = resolved.isPresent() ? resolved.translation().getDescription() : null;
        Map<String, com.spexcrafters.taxonomy.domain.TranslationStatus> statuses = new LinkedHashMap<>();
        rows.forEach(t -> statuses.put(t.getLocale(), t.getTranslationStatus()));
        String enumerationCode = attr.getEnumerationId() == null ? null
                : enumerations.findById(attr.getEnumerationId()).map(Enumeration::getCode).orElse(null);
        return new AttributeDetail(attr.getId(), attr.getCode(), attr.getDataType(), attr.getUnitCode(),
                enumerationCode, attr.getMinValue(), attr.getMaxValue(), attr.getMinLength(),
                attr.getMaxLength(), attr.getRegexPattern(), attr.isSearchable(), attr.isFilterable(),
                attr.isSortable(), attr.isComparable(), attr.isFacetable(), attr.isSeo(), attr.isVisible(),
                attr.isDeprecated(), attr.getSortOrder(), name, description, statuses, attr.getVersion());
    }

    private String resolveName(Attribute attr, List<AttributeTranslation> rows, String locale) {
        var resolved = LocalizationResolver.resolve(rows, locale, attr.getSourceVersion());
        return resolved.isPresent() && resolved.translation().getName() != null
                ? resolved.translation().getName() : attr.getCode();
    }

    private String requireLocale(String raw, String field) {
        return SupportedLocale.parse(raw).map(SupportedLocale::code).orElseThrow(() ->
                ApiProblemException.validation(List.of(new ProblemFieldError(
                        field, "unsupported-locale", "Unsupported locale: " + raw))));
    }

    private void requireVersion(Attribute attr, int expectedVersion) {
        if (attr.getVersion() != null && attr.getVersion() != expectedVersion) {
            throw new TaxonomyConflictException("Version mismatch: expected " + attr.getVersion()
                    + " but was " + expectedVersion + ".");
        }
    }
}
