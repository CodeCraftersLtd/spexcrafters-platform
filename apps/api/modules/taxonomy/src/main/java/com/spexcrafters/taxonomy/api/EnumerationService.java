package com.spexcrafters.taxonomy.api;

import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.platformaccess.api.PlatformCapability;
import com.spexcrafters.platformaccess.api.PlatformAccess;
import com.spexcrafters.sharedkernel.i18n.SupportedLocale;
import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import com.spexcrafters.sharedkernel.problem.ProblemFieldError;
import com.spexcrafters.sharedkernel.util.UuidV7;
import com.spexcrafters.taxonomy.domain.Enumeration;
import com.spexcrafters.taxonomy.domain.EnumerationValue;
import com.spexcrafters.taxonomy.domain.EnumerationValueTranslation;
import com.spexcrafters.taxonomy.domain.TaxonomyConflictException;
import com.spexcrafters.taxonomy.domain.TaxonomyNotFoundException;
import com.spexcrafters.taxonomy.domain.TranslationSource;
import com.spexcrafters.taxonomy.infrastructure.EnumerationRepository;
import com.spexcrafters.taxonomy.infrastructure.EnumerationValueRepository;
import com.spexcrafters.taxonomy.infrastructure.EnumerationValueTranslationRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Enumeration registry reads and administration (create enum, add value, translate value). */
@Service
public class EnumerationService {

    private final EnumerationRepository enumerations;
    private final EnumerationValueRepository values;
    private final EnumerationValueTranslationRepository translations;
    private final PlatformAccess platformAccess;
    private final AuditLogger audit;

    public EnumerationService(EnumerationRepository enumerations, EnumerationValueRepository values,
            EnumerationValueTranslationRepository translations, PlatformAccess platformAccess,
            AuditLogger audit) {
        this.enumerations = enumerations;
        this.values = values;
        this.translations = translations;
        this.platformAccess = platformAccess;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<EnumerationSummary> list(String locale) {
        // Public read: only active enumerations are exposed.
        return enumerations.findAllByOrderByCodeAsc().stream()
                .filter(Enumeration::isActive)
                .map(e -> new EnumerationSummary(e.getId(), e.getCode(), e.isActive(),
                        (int) values.countByEnumerationId(e.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public EnumerationDetail get(String code, String locale) {
        // Public read: an inactive enumeration is hidden (404), and inactive/deprecated values
        // are excluded from the detail.
        Enumeration enumeration = enumerations.findByCode(code)
                .filter(Enumeration::isActive)
                .orElseThrow(TaxonomyNotFoundException::new);
        return toDetail(enumeration, locale, false);
    }

    /**
     * Administration enumeration list: platform-staff-only (TAXONOMY_READ). Returns EVERY
     * enumeration (including inactive) with EVERY value (including inactive/deprecated), each
     * carrying its stable {@code id}, so staff can administer values regardless of status.
     */
    @Transactional(readOnly = true)
    public List<EnumerationDetail> listForAdmin(UUID userId, String locale) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_READ);
        return enumerations.findAllByOrderByCodeAsc().stream()
                .map(e -> toDetail(e, locale, true))
                .toList();
    }

    @Transactional
    public EnumerationDetail create(UUID userId, String code) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_WRITE);
        if (enumerations.existsByCode(code)) {
            throw new TaxonomyConflictException("An enumeration with code " + code + " already exists.");
        }
        Enumeration enumeration = new Enumeration(UuidV7.generate(), code, null);
        enumeration.setCreatedBy(userId);
        enumeration.setUpdatedBy(userId);
        enumerations.save(enumeration);
        audit.record("taxonomy.enumeration.created", userId, "enumeration", enumeration.getId().toString(),
                Map.of("code", code));
        return new EnumerationDetail(enumeration.getId(), enumeration.getCode(), enumeration.isActive(),
                List.of());
    }

    @Transactional
    public EnumerationValueView addValue(UUID userId, UUID enumerationId, String code, Integer sortOrder,
            String originalLocaleRaw, String label, String description) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_WRITE);
        Enumeration enumeration = enumerations.findById(enumerationId)
                .orElseThrow(TaxonomyNotFoundException::new);
        if (values.existsByEnumerationIdAndCode(enumerationId, code)) {
            throw new TaxonomyConflictException("Value " + code + " already exists in this enumeration.");
        }
        String originalLocale = requireLocale(originalLocaleRaw, "originalLocale");
        EnumerationValue value = new EnumerationValue(UuidV7.generate(), enumerationId, code,
                sortOrder == null ? 0 : sortOrder);
        value.setCreatedBy(userId);
        value.setUpdatedBy(userId);
        values.save(value);

        EnumerationValueTranslation original = new EnumerationValueTranslation(UuidV7.generate(),
                value.getId(), originalLocale, originalLocale, 1, TranslationSource.HUMAN, true, userId);
        original.applyContent(label, description, 1, TranslationSource.HUMAN, userId);
        original.setCreatedBy(userId);
        original.setUpdatedBy(userId);
        translations.save(original);

        audit.record("taxonomy.enumeration.value.added", userId, "enumeration_value",
                value.getId().toString(), Map.of("enumerationId", enumerationId.toString(), "code", code));
        return new EnumerationValueView(value.getId(), value.getCode(), label, value.getSortOrder(),
                value.isDeprecated(), value.isActive());
    }

    @Transactional
    public TranslationView upsertValueTranslation(UUID userId, UUID valueId, String localeRaw, String label,
            String description, TranslationSource sourceRaw) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_WRITE);
        EnumerationValue value = values.findById(valueId).orElseThrow(TaxonomyNotFoundException::new);
        String locale = requireLocale(localeRaw, "locale");
        List<EnumerationValueTranslation> rows = translations.findByEnumerationValueId(valueId);
        Optional<EnumerationValueTranslation> originalRow = rows.stream()
                .filter(EnumerationValueTranslation::isOriginal).findFirst();
        boolean isOriginal = originalRow.map(o -> o.getLocale().equals(locale)).orElse(false);
        TranslationSource source = sourceRaw == null ? TranslationSource.HUMAN : sourceRaw;
        if (isOriginal && source == TranslationSource.MACHINE) {
            throw ApiProblemException.validation(List.of(new ProblemFieldError(
                    "source", "invalid-source", "Original-language content cannot be machine-translated.")));
        }
        String originalLocale = originalRow.map(EnumerationValueTranslation::getLocale)
                .orElse(SupportedLocale.FALLBACK.code());
        // enumeration_value has no parent source_version column; the original row's own version is the counter.
        int currentVersion = originalRow.map(EnumerationValueTranslation::getSourceVersion).orElse(1);
        int targetVersion = isOriginal ? currentVersion + 1 : currentVersion;

        Optional<EnumerationValueTranslation> existing = rows.stream()
                .filter(t -> t.getLocale().equals(locale)).findFirst();
        EnumerationValueTranslation translation = existing.orElseGet(() -> {
            EnumerationValueTranslation created = new EnumerationValueTranslation(UuidV7.generate(), valueId,
                    locale, originalLocale, targetVersion, source, false, userId);
            created.setCreatedBy(userId);
            return created;
        });
        translation.applyContent(label, description, targetVersion, source, userId);
        translation.setUpdatedBy(userId);
        translations.save(translation);
        int currentAfter = isOriginal ? targetVersion : currentVersion;
        audit.record("taxonomy.enumeration.value.translation.upserted", userId,
                "enumeration_value_translation", translation.getId().toString(),
                Map.of("valueId", valueId.toString(), "locale", locale));
        return new TranslationView(translation.getLocale(), translation.getLabel(),
                translation.getDescription(), translation.getTranslationStatus(), translation.isOriginal(),
                translation.isStale(currentAfter), translation.getSourceVersion());
    }

    private EnumerationDetail toDetail(Enumeration enumeration, String locale, boolean includeNonPublic) {
        List<EnumerationValue> vals = values.findByEnumerationIdOrderBySortOrderAsc(enumeration.getId()).stream()
                .filter(v -> includeNonPublic || (v.isActive() && !v.isDeprecated()))
                .toList();
        List<UUID> valueIds = vals.stream().map(EnumerationValue::getId).toList();
        Map<UUID, List<EnumerationValueTranslation>> byValue = valueIds.isEmpty() ? Map.of()
                : translations.findByEnumerationValueIdIn(valueIds).stream()
                        .collect(Collectors.groupingBy(EnumerationValueTranslation::getEnumerationValueId));
        List<EnumerationValueView> views = new ArrayList<>();
        for (EnumerationValue v : vals) {
            List<EnumerationValueTranslation> rows = byValue.getOrDefault(v.getId(), List.of());
            int currentVersion = rows.stream().filter(EnumerationValueTranslation::isOriginal)
                    .mapToInt(EnumerationValueTranslation::getSourceVersion).findFirst().orElse(1);
            var resolved = LocalizationResolver.resolve(rows, locale, currentVersion);
            String label = resolved.isPresent() && resolved.translation().getLabel() != null
                    ? resolved.translation().getLabel() : v.getCode();
            views.add(new EnumerationValueView(v.getId(), v.getCode(), label, v.getSortOrder(),
                    v.isDeprecated(), v.isActive()));
        }
        return new EnumerationDetail(enumeration.getId(), enumeration.getCode(), enumeration.isActive(), views);
    }

    private String requireLocale(String raw, String field) {
        return SupportedLocale.parse(raw).map(SupportedLocale::code).orElseThrow(() ->
                ApiProblemException.validation(List.of(new ProblemFieldError(
                        field, "unsupported-locale", "Unsupported locale: " + raw))));
    }
}
