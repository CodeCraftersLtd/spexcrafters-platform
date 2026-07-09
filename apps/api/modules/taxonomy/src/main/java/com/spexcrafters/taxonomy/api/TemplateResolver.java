package com.spexcrafters.taxonomy.api;

import com.spexcrafters.taxonomy.domain.Category;
import com.spexcrafters.taxonomy.domain.SpecificationTemplate;
import com.spexcrafters.taxonomy.domain.SpecificationTemplateAttribute;
import com.spexcrafters.taxonomy.infrastructure.AttributeRepository;
import com.spexcrafters.taxonomy.infrastructure.CategoryRepository;
import com.spexcrafters.taxonomy.infrastructure.SpecificationTemplateAttributeRepository;
import com.spexcrafters.taxonomy.infrastructure.SpecificationTemplateRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves a category's <em>effective</em> specification template (ADR-025 §3): its own
 * template attributes plus those inherited from ancestor categories, de-duplicated by attribute
 * code with the nearest-ancestor definition winning. The merge itself ({@link #merge}) is pure
 * and exhaustively unit-tested; the service wrapper loads the ancestor chain from the DB.
 */
@Service
public class TemplateResolver {

    private final CategoryRepository categories;
    private final SpecificationTemplateRepository templates;
    private final SpecificationTemplateAttributeRepository templateAttributes;
    private final AttributeRepository attributes;

    public TemplateResolver(CategoryRepository categories, SpecificationTemplateRepository templates,
            SpecificationTemplateAttributeRepository templateAttributes, AttributeRepository attributes) {
        this.categories = categories;
        this.templates = templates;
        this.templateAttributes = templateAttributes;
        this.attributes = attributes;
    }

    /** A single template attribute slot before inheritance resolution. */
    public record SlotDef(String attributeCode, boolean required, String conditionalJson, String defaultValue,
            int sortOrder) {
    }

    /** One category's contribution to the effective template (nearest ancestor first). */
    public record CategoryLayer(String categoryCode, boolean self, List<SlotDef> slots) {
    }

    /** A resolved effective-template slot, tagged with whether/where it was inherited. */
    public record ResolvedSlot(String attributeCode, boolean required, String conditionalJson,
            String defaultValue, int sortOrder, boolean inherited, String sourceCategoryCode) {
    }

    /**
     * Pure inheritance resolution: given the category's own layer plus ancestor layers ordered
     * nearest-first, keep the first definition seen for each attribute code (nearest wins),
     * ordering own slots before inherited ones, each by sort order then code.
     */
    public static List<ResolvedSlot> merge(List<CategoryLayer> layersNearestFirst) {
        Map<String, ResolvedSlot> byCode = new LinkedHashMap<>();
        for (CategoryLayer layer : layersNearestFirst) {
            for (SlotDef slot : layer.slots()) {
                byCode.computeIfAbsent(slot.attributeCode(), code -> new ResolvedSlot(
                        code, slot.required(), slot.conditionalJson(), slot.defaultValue(), slot.sortOrder(),
                        !layer.self(), layer.categoryCode()));
            }
        }
        List<ResolvedSlot> result = new ArrayList<>(byCode.values());
        result.sort(Comparator.comparing(ResolvedSlot::inherited)
                .thenComparingInt(ResolvedSlot::sortOrder)
                .thenComparing(ResolvedSlot::attributeCode));
        return result;
    }

    /** The effective slots for {@code categoryCode}, or empty when the category has no template chain. */
    @Transactional(readOnly = true)
    public List<ResolvedSlot> effectiveSlots(Category category) {
        List<CategoryLayer> layers = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        Category current = category;
        while (current != null && visited.add(current.getId())) {
            boolean self = current.getId().equals(category.getId());
            layers.add(new CategoryLayer(current.getCode(), self, slotDefs(current.getId())));
            current = current.getParentId() == null ? null
                    : categories.findById(current.getParentId()).orElse(null);
        }
        return merge(layers);
    }

    /** The code of the category's own template (not inherited), if any. */
    @Transactional(readOnly = true)
    public Optional<String> ownTemplateCode(UUID categoryId) {
        return templates.findByCategoryId(categoryId).map(SpecificationTemplate::getCode);
    }

    private List<SlotDef> slotDefs(UUID categoryId) {
        Optional<SpecificationTemplate> template = templates.findByCategoryId(categoryId);
        if (template.isEmpty()) {
            return List.of();
        }
        List<SlotDef> defs = new ArrayList<>();
        for (SpecificationTemplateAttribute ta
                : templateAttributes.findByTemplateIdOrderBySortOrderAsc(template.get().getId())) {
            attributes.findById(ta.getAttributeId()).ifPresent(attr -> defs.add(new SlotDef(
                    attr.getCode(), ta.isRequired(), ta.getConditional(), ta.getDefaultValue(),
                    ta.getSortOrder())));
        }
        return defs;
    }
}
