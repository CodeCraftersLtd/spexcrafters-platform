package com.spexcrafters.taxonomy.web;

import com.spexcrafters.taxonomy.domain.CategoryClassification;
import jakarta.validation.constraints.NotNull;

/** Body of {@code updateCategory}. */
public record UpdateCategoryRequest(
        String parentCode,
        CategoryClassification classification,
        Integer sortOrder,
        @NotNull Integer version) {
}
