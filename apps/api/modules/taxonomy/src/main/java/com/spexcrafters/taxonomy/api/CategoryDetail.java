package com.spexcrafters.taxonomy.api;

import com.spexcrafters.taxonomy.domain.CategoryClassification;
import java.util.List;
import java.util.UUID;

/** A single category by its stable code (CategoryDetail schema). */
public record CategoryDetail(
        UUID id,
        String code,
        String parentCode,
        CategoryClassification classification,
        int depth,
        String path,
        boolean active,
        int sortOrder,
        String name,
        String description,
        String primarySlug,
        int version,
        List<TaxonomyCapability> callerCapabilities) {
}
