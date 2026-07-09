package com.spexcrafters.taxonomy.api;

import com.spexcrafters.taxonomy.domain.CategoryClassification;
import java.util.List;

/** A node of the localized, nested public category tree (operationId getCategoryTree). */
public record CategoryTreeNode(
        String code,
        CategoryClassification classification,
        String name,
        boolean active,
        int sortOrder,
        String slug,
        List<CategoryTreeNode> children) {
}
