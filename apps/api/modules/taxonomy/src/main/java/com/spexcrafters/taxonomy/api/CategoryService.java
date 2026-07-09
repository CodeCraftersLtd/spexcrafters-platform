package com.spexcrafters.taxonomy.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.platformaccess.api.PlatformAccess;
import com.spexcrafters.platformaccess.api.PlatformCapability;
import com.spexcrafters.platformaccess.api.PlatformStaffContext;
import com.spexcrafters.sharedkernel.i18n.SupportedLocale;
import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import com.spexcrafters.sharedkernel.problem.ProblemFieldError;
import com.spexcrafters.sharedkernel.util.UuidV7;
import com.spexcrafters.taxonomy.domain.Attribute;
import com.spexcrafters.taxonomy.domain.Category;
import com.spexcrafters.taxonomy.domain.CategoryClassification;
import com.spexcrafters.taxonomy.domain.CategorySlug;
import com.spexcrafters.taxonomy.domain.CategoryTranslation;
import com.spexcrafters.taxonomy.domain.Enumeration;
import com.spexcrafters.taxonomy.domain.TaxonomyConflictException;
import com.spexcrafters.taxonomy.domain.TaxonomyNotFoundException;
import com.spexcrafters.taxonomy.domain.TranslationSource;
import com.spexcrafters.taxonomy.infrastructure.AttributeRepository;
import com.spexcrafters.taxonomy.infrastructure.AttributeTranslationRepository;
import com.spexcrafters.taxonomy.infrastructure.CategoryRepository;
import com.spexcrafters.taxonomy.infrastructure.CategorySlugRepository;
import com.spexcrafters.taxonomy.infrastructure.CategoryTranslationRepository;
import com.spexcrafters.taxonomy.infrastructure.EnumerationRepository;
import com.spexcrafters.taxonomy.infrastructure.SpecificationTemplateAttributeRepository;
import com.spexcrafters.taxonomy.infrastructure.SpecificationTemplateRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Category tree reads, category administration, translations and specification-template replacement. */
@Service
public class CategoryService {

    private final CategoryRepository categories;
    private final CategoryTranslationRepository translations;
    private final CategorySlugRepository slugs;
    private final SpecificationTemplateRepository templates;
    private final SpecificationTemplateAttributeRepository templateAttributes;
    private final AttributeRepository attributes;
    private final AttributeTranslationRepository attributeTranslations;
    private final EnumerationRepository enumerations;
    private final TemplateResolver templateResolver;
    private final PlatformAccess platformAccess;
    private final AuditLogger audit;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public CategoryService(CategoryRepository categories, CategoryTranslationRepository translations,
            CategorySlugRepository slugs, SpecificationTemplateRepository templates,
            SpecificationTemplateAttributeRepository templateAttributes, AttributeRepository attributes,
            AttributeTranslationRepository attributeTranslations, EnumerationRepository enumerations,
            TemplateResolver templateResolver, PlatformAccess platformAccess, AuditLogger audit,
            ObjectMapper objectMapper, Clock clock) {
        this.categories = categories;
        this.translations = translations;
        this.slugs = slugs;
        this.templates = templates;
        this.templateAttributes = templateAttributes;
        this.attributes = attributes;
        this.attributeTranslations = attributeTranslations;
        this.enumerations = enumerations;
        this.templateResolver = templateResolver;
        this.platformAccess = platformAccess;
        this.audit = audit;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ public reads

    @Transactional(readOnly = true)
    public List<CategoryTreeNode> getTree(String locale) {
        List<Category> active = categories.findByActiveTrueOrderBySortOrderAsc();
        List<UUID> ids = active.stream().map(Category::getId).toList();
        Map<UUID, List<CategoryTranslation>> byCat = translations.findByCategoryIdIn(ids).stream()
                .collect(Collectors.groupingBy(CategoryTranslation::getCategoryId));
        Map<UUID, List<CategorySlug>> slugByCat = slugs.findByPrimaryTrueAndActiveTrue().stream()
                .collect(Collectors.groupingBy(CategorySlug::getCategoryId));

        Map<UUID, List<Category>> children = active.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));
        List<CategoryTreeNode> roots = new ArrayList<>();
        for (Category root : active.stream().filter(c -> c.getParentId() == null).toList()) {
            roots.add(toNode(root, locale, byCat, slugByCat, children));
        }
        return roots;
    }

    private CategoryTreeNode toNode(Category category, String locale,
            Map<UUID, List<CategoryTranslation>> byCat, Map<UUID, List<CategorySlug>> slugByCat,
            Map<UUID, List<Category>> children) {
        List<CategoryTreeNode> childNodes = new ArrayList<>();
        for (Category child : children.getOrDefault(category.getId(), List.of())) {
            childNodes.add(toNode(child, locale, byCat, slugByCat, children));
        }
        Resolved resolved = resolveName(category, byCat.getOrDefault(category.getId(), List.of()), locale);
        return new CategoryTreeNode(category.getCode(), category.getClassification(), resolved.name(),
                category.isActive(), category.getSortOrder(),
                primarySlug(slugByCat.getOrDefault(category.getId(), List.of()), locale), childNodes);
    }

    @Transactional(readOnly = true)
    public CategoryDetail getCategory(String code, String locale) {
        Category category = categories.findByCode(code).orElseThrow(TaxonomyNotFoundException::new);
        return toDetail(category, locale, null);
    }

    @Transactional(readOnly = true)
    public EffectiveSpecificationTemplate getEffectiveTemplate(String code, String locale) {
        Category category = categories.findByCode(code).orElseThrow(TaxonomyNotFoundException::new);
        return buildEffectiveTemplate(category, locale);
    }

    // ------------------------------------------------------------------ administration reads

    /**
     * Administration category list: platform-staff-only (TAXONOMY_READ). Returns EVERY category
     * (including inactive) as a FLAT list ordered by materialized {@code path}, each carrying its
     * stable {@code id} and {@code parentCode}. This single read also supplies the dashboard's
     * code -> id map, replacing the per-category detail fan-out.
     */
    @Transactional(readOnly = true)
    public List<CategoryDetail> listForAdmin(UUID userId, String locale) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_READ);
        List<Category> all = categories.findAllByOrderByPathAsc();
        Map<UUID, String> codeById = all.stream()
                .collect(Collectors.toMap(Category::getId, Category::getCode));
        List<UUID> ids = all.stream().map(Category::getId).toList();
        Map<UUID, List<CategoryTranslation>> byCat = ids.isEmpty() ? Map.of()
                : translations.findByCategoryIdIn(ids).stream()
                        .collect(Collectors.groupingBy(CategoryTranslation::getCategoryId));
        List<CategoryDetail> result = new ArrayList<>();
        for (Category category : all) {
            Resolved resolved = resolveName(category,
                    byCat.getOrDefault(category.getId(), List.of()), locale);
            String parentCode = category.getParentId() == null ? null
                    : codeById.get(category.getParentId());
            result.add(new CategoryDetail(category.getId(), category.getCode(), parentCode,
                    category.getClassification(), category.getDepth(), category.getPath(),
                    category.isActive(), category.getSortOrder(), resolved.name(), resolved.description(),
                    null, category.getVersion(), null));
        }
        return result;
    }

    /**
     * Administration effective-template read keyed by category uuid (public read keys by code).
     * Platform-staff-only (TAXONOMY_READ); unknown id -> 404.
     */
    @Transactional(readOnly = true)
    public EffectiveSpecificationTemplate getEffectiveTemplateForAdmin(UUID userId, UUID categoryId,
            String locale) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_READ);
        Category category = categories.findById(categoryId).orElseThrow(TaxonomyNotFoundException::new);
        return buildEffectiveTemplate(category, locale);
    }

    /**
     * All translation rows for a category, in EVERY locale and status (draft/approved/rejected),
     * so staff can inspect the translation state. Platform-staff-only (TAXONOMY_READ).
     */
    @Transactional(readOnly = true)
    public List<TranslationView> listTranslations(UUID userId, UUID categoryId) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_READ);
        Category category = categories.findById(categoryId).orElseThrow(TaxonomyNotFoundException::new);
        return translations.findByCategoryId(categoryId).stream()
                .map(t -> toTranslationView(t, category.getSourceVersion()))
                .toList();
    }

    // ------------------------------------------------------------------ administration

    @Transactional
    public CategoryDetail create(UUID userId, String code, String parentCode,
            CategoryClassification classification, String originalLocaleRaw, String name, Integer sortOrder) {
        PlatformStaffContext ctx = platformAccess.require(userId, PlatformCapability.TAXONOMY_WRITE);
        if (categories.existsByCode(code)) {
            throw new TaxonomyConflictException("A category with code " + code + " already exists.");
        }
        String originalLocale = requireLocale(originalLocaleRaw, "originalLocale");
        int depth;
        String path;
        if (parentCode == null || parentCode.isBlank()) {
            depth = 0;
            path = "/" + code + "/";
        } else {
            Category parent = categories.findByCode(parentCode).orElseThrow(() ->
                    ApiProblemException.validation(List.of(new ProblemFieldError(
                            "parentCode", "unknown-reference", "Unknown parent category: " + parentCode))));
            depth = parent.getDepth() + 1;
            path = parent.getPath() + code + "/";
        }
        Category category = new Category(UuidV7.generate(), parentCode == null || parentCode.isBlank() ? null
                : categories.findByCode(parentCode).map(Category::getId).orElseThrow(),
                code, depth, path, classification, sortOrder == null ? 0 : sortOrder);
        category.setCreatedBy(userId);
        category.setUpdatedBy(userId);
        categories.save(category);

        CategoryTranslation original = new CategoryTranslation(UuidV7.generate(), category.getId(),
                originalLocale, originalLocale, category.getSourceVersion(), TranslationSource.HUMAN, true, userId);
        original.applyContent(name, null, category.getSourceVersion(), TranslationSource.HUMAN, userId);
        original.setCreatedBy(userId);
        original.setUpdatedBy(userId);
        translations.save(original);

        String slug = SlugGenerator.uniqueSlug(name,
                s -> slugs.findByLocaleAndSlug(originalLocale, s).isPresent());
        CategorySlug slugRow = new CategorySlug(UuidV7.generate(), category.getId(), originalLocale, slug, true);
        slugRow.setCreatedBy(userId);
        slugRow.setUpdatedBy(userId);
        slugs.save(slugRow);

        audit.record("taxonomy.category.created", userId, "category", category.getId().toString(),
                Map.of("code", code));
        return toDetail(category, originalLocale, ctx);
    }

    @Transactional
    public CategoryDetail update(UUID userId, UUID id, String parentCode, CategoryClassification classification,
            Integer sortOrder, int expectedVersion) {
        PlatformStaffContext ctx = platformAccess.require(userId, PlatformCapability.TAXONOMY_WRITE);
        Category category = categories.findById(id).orElseThrow(TaxonomyNotFoundException::new);
        requireVersion(category, expectedVersion);
        if (classification != null) {
            category.setClassification(classification);
        }
        if (sortOrder != null) {
            category.setSortOrder(sortOrder);
        }
        if (parentCode != null && !parentCode.isBlank()) {
            reparent(category, parentCode);
        }
        category.setUpdatedBy(userId);
        audit.record("taxonomy.category.updated", userId, "category", id.toString(),
                Map.of("code", category.getCode()));
        return toDetail(category, category.getCode(), ctx);
    }

    private void reparent(Category category, String parentCode) {
        Category parent = categories.findByCode(parentCode).orElseThrow(() ->
                ApiProblemException.validation(List.of(new ProblemFieldError(
                        "parentCode", "unknown-reference", "Unknown parent category: " + parentCode))));
        if (parent.getId().equals(category.getId())
                || parent.getPath().contains("/" + category.getCode() + "/")) {
            throw ApiProblemException.validation(List.of(new ProblemFieldError(
                    "parentCode", "cycle", "Re-parenting would create a cycle.")));
        }
        String oldPath = category.getPath();
        int oldDepth = category.getDepth();
        String newPath = parent.getPath() + category.getCode() + "/";
        int newDepth = parent.getDepth() + 1;
        category.reparent(parent.getId(), newDepth, newPath);
        // Rebuild descendant paths/depths so the materialized tree stays consistent.
        for (Category descendant : categories.findAll()) {
            if (!descendant.getId().equals(category.getId()) && descendant.getPath().startsWith(oldPath)) {
                String rebuilt = newPath + descendant.getPath().substring(oldPath.length());
                descendant.reparent(descendant.getParentId(),
                        descendant.getDepth() + (newDepth - oldDepth), rebuilt);
            }
        }
    }

    @Transactional
    public CategoryDetail setActivation(UUID userId, UUID id, boolean active) {
        PlatformStaffContext ctx = platformAccess.require(userId, PlatformCapability.TAXONOMY_WRITE);
        Category category = categories.findById(id).orElseThrow(TaxonomyNotFoundException::new);
        category.setActive(active);
        category.setUpdatedBy(userId);
        audit.record(active ? "taxonomy.category.activated" : "taxonomy.category.deactivated",
                userId, "category", id.toString(), Map.of("code", category.getCode()));
        return toDetail(category, category.getCode(), ctx);
    }

    @Transactional
    public TranslationView upsertTranslation(UUID userId, UUID id, String localeRaw, String name,
            String description, TranslationSource sourceRaw) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_WRITE);
        Category category = categories.findById(id).orElseThrow(TaxonomyNotFoundException::new);
        String locale = requireLocale(localeRaw, "locale");
        boolean isOriginal = translations.findByCategoryId(id).stream()
                .anyMatch(t -> t.isOriginal() && t.getLocale().equals(locale));
        TranslationSource source = sourceRaw == null ? TranslationSource.HUMAN : sourceRaw;
        if (isOriginal && source == TranslationSource.MACHINE) {
            throw ApiProblemException.validation(List.of(new ProblemFieldError(
                    "source", "invalid-source", "Original-language content cannot be machine-translated.")));
        }
        int targetVersion = isOriginal ? category.bumpSourceVersion() : category.getSourceVersion();

        Optional<CategoryTranslation> existing = translations.findByCategoryId(id).stream()
                .filter(t -> t.getLocale().equals(locale)).findFirst();
        CategoryTranslation translation = existing.orElseGet(() -> {
            CategoryTranslation created = new CategoryTranslation(UuidV7.generate(), id, locale,
                    originalLocaleOf(id), targetVersion, source, false, userId);
            created.setCreatedBy(userId);
            return created;
        });
        translation.applyContent(name, description, targetVersion, source, userId);
        translation.setUpdatedBy(userId);
        translations.save(translation);
        category.setUpdatedBy(userId);
        audit.record("taxonomy.category.translation.upserted", userId, "category_translation",
                translation.getId().toString(), Map.of("categoryId", id.toString(), "locale", locale));
        return toTranslationView(translation, category.getSourceVersion());
    }

    @Transactional
    public TranslationView approveTranslation(UUID userId, UUID id, String localeRaw) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_WRITE);
        Category category = categories.findById(id).orElseThrow(TaxonomyNotFoundException::new);
        String locale = requireLocale(localeRaw, "locale");
        CategoryTranslation translation = translations.findByCategoryId(id).stream()
                .filter(t -> t.getLocale().equals(locale)).findFirst()
                .orElseThrow(TaxonomyNotFoundException::new);
        translation.approve(userId, clock.instant());
        translation.setUpdatedBy(userId);
        audit.record("taxonomy.category.translation.approved", userId, "category_translation",
                translation.getId().toString(), Map.of("categoryId", id.toString(), "locale", locale));
        return toTranslationView(translation, category.getSourceVersion());
    }

    @Transactional
    public EffectiveSpecificationTemplate putSpecificationTemplate(UUID userId, UUID categoryId, String code,
            List<TemplateAttributeInput> attributeInputs) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_WRITE);
        Category category = categories.findById(categoryId).orElseThrow(TaxonomyNotFoundException::new);

        var existing = templates.findByCategoryId(categoryId);
        var template = existing.orElse(null);
        if (template == null) {
            if (templates.existsByCode(code)) {
                throw new TaxonomyConflictException("Template code " + code + " already exists.");
            }
            template = new com.spexcrafters.taxonomy.domain.SpecificationTemplate(
                    UuidV7.generate(), categoryId, code);
            template.setCreatedBy(userId);
        } else {
            if (!template.getCode().equals(code) && templates.existsByCode(code)) {
                throw new TaxonomyConflictException("Template code " + code + " already exists.");
            }
            template.bumpTemplateVersion();
            templateAttributes.deleteByTemplateId(template.getId());
        }
        template.setUpdatedBy(userId);
        templates.save(template);

        for (TemplateAttributeInput input : attributeInputs) {
            Attribute attribute = attributes.findByCode(input.attributeCode()).orElseThrow(() ->
                    ApiProblemException.validation(List.of(new ProblemFieldError(
                            "attributeCode", "unknown-reference",
                            "Unknown attribute: " + input.attributeCode()))));
            var slot = new com.spexcrafters.taxonomy.domain.SpecificationTemplateAttribute(
                    UuidV7.generate(), template.getId(), attribute.getId(), input.required(),
                    serializeConditional(input.conditional()), input.defaultValue(),
                    input.sortOrder() == null ? 0 : input.sortOrder());
            slot.setCreatedBy(userId);
            slot.setUpdatedBy(userId);
            templateAttributes.save(slot);
        }
        audit.record("taxonomy.category.template.replaced", userId, "specification_template",
                template.getId().toString(), Map.of("categoryId", categoryId.toString(), "code", code));
        return buildEffectiveTemplate(category, category.getCode());
    }

    /** Structural input for one template slot (from the web request). */
    public record TemplateAttributeInput(String attributeCode, boolean required,
            Map<String, Object> conditional, String defaultValue, Integer sortOrder) {
    }

    // ------------------------------------------------------------------ mapping helpers

    private EffectiveSpecificationTemplate buildEffectiveTemplate(Category category, String locale) {
        List<TemplateResolver.ResolvedSlot> slots = templateResolver.effectiveSlots(category);
        List<SpecificationTemplateAttributeView> views = new ArrayList<>();
        for (TemplateResolver.ResolvedSlot slot : slots) {
            Attribute attr = attributes.findByCode(slot.attributeCode()).orElse(null);
            if (attr == null) {
                continue;
            }
            String enumerationCode = attr.getEnumerationId() == null ? null
                    : enumerations.findById(attr.getEnumerationId()).map(Enumeration::getCode).orElse(null);
            String name = resolveAttributeName(attr, locale);
            views.add(new SpecificationTemplateAttributeView(attr.getCode(), attr.getDataType(), name,
                    attr.getUnitCode(), enumerationCode, slot.required(), slot.inherited(),
                    slot.sourceCategoryCode(), deserializeConditional(slot.conditionalJson()),
                    slot.defaultValue(), slot.sortOrder()));
        }
        String templateCode = templateResolver.ownTemplateCode(category.getId()).orElse(null);
        return new EffectiveSpecificationTemplate(category.getCode(), templateCode, views);
    }

    private String resolveAttributeName(Attribute attr, String locale) {
        var resolved = LocalizationResolver.resolve(
                attributeTranslations.findByAttributeId(attr.getId()), locale, attr.getSourceVersion());
        return resolved.isPresent() && resolved.translation().getName() != null
                ? resolved.translation().getName() : attr.getCode();
    }

    private CategoryDetail toDetail(Category category, String locale, PlatformStaffContext ctx) {
        Resolved resolved = resolveName(category, translations.findByCategoryId(category.getId()), locale);
        String parentCode = category.getParentId() == null ? null
                : categories.findById(category.getParentId()).map(Category::getCode).orElse(null);
        String primarySlug = primarySlug(
                slugs.findByCategoryIdAndLocale(category.getId(), SupportedLocale.normalizeOrFallback(locale)),
                locale);
        if (primarySlug == null) {
            primarySlug = slugs
                    .findByCategoryIdAndLocaleAndPrimaryTrueAndActiveTrue(category.getId(),
                            SupportedLocale.FALLBACK.code())
                    .map(CategorySlug::getSlug).orElse(null);
        }
        List<TaxonomyCapability> caps = ctx == null ? null : TaxonomyCapabilities.of(ctx);
        return new CategoryDetail(category.getId(), category.getCode(), parentCode,
                category.getClassification(), category.getDepth(), category.getPath(), category.isActive(),
                category.getSortOrder(), resolved.name(), resolved.description(), primarySlug,
                category.getVersion(), caps);
    }

    private Resolved resolveName(Category category, List<CategoryTranslation> catTranslations, String locale) {
        var resolved = LocalizationResolver.resolve(catTranslations, locale, category.getSourceVersion());
        if (!resolved.isPresent()) {
            return new Resolved(category.getCode(), null);
        }
        CategoryTranslation t = resolved.translation();
        return new Resolved(t.getName() != null ? t.getName() : category.getCode(), t.getDescription());
    }

    private String primarySlug(List<CategorySlug> categorySlugs, String locale) {
        String requested = SupportedLocale.normalizeOrFallback(locale);
        return categorySlugs.stream()
                .filter(s -> s.isPrimary() && s.isActive() && s.getLocale().equals(requested))
                .map(CategorySlug::getSlug).findFirst().orElse(null);
    }

    private String originalLocaleOf(UUID categoryId) {
        return translations.findByCategoryId(categoryId).stream().filter(CategoryTranslation::isOriginal)
                .map(CategoryTranslation::getLocale).findFirst().orElse(SupportedLocale.FALLBACK.code());
    }

    private TranslationView toTranslationView(CategoryTranslation t, int currentSourceVersion) {
        return new TranslationView(t.getLocale(), t.getName(), t.getDescription(), t.getTranslationStatus(),
                t.isOriginal(), t.isStale(currentSourceVersion), t.getSourceVersion());
    }

    private String serializeConditional(Map<String, Object> conditional) {
        if (conditional == null || conditional.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(conditional);
        } catch (Exception ex) {
            throw ApiProblemException.validation(List.of(new ProblemFieldError(
                    "conditional", "invalid-json", "Conditional rule is not serializable.")));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializeConditional(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private String requireLocale(String raw, String field) {
        return SupportedLocale.parse(raw).map(SupportedLocale::code).orElseThrow(() ->
                ApiProblemException.validation(List.of(new ProblemFieldError(
                        field, "unsupported-locale", "Unsupported locale: " + raw))));
    }

    private void requireVersion(Category category, int expectedVersion) {
        if (category.getVersion() != null && category.getVersion() != expectedVersion) {
            throw new TaxonomyConflictException("Version mismatch: expected " + category.getVersion()
                    + " but was " + expectedVersion + ".");
        }
    }

    private record Resolved(String name, String description) {
    }

    // Kept for symmetry with other services that index translations by id.
    @SuppressWarnings("unused")
    private static <T> Map<UUID, T> index(List<T> items, Function<T, UUID> key) {
        return items.stream().collect(Collectors.toMap(key, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }
}
