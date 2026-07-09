package com.spexcrafters.taxonomy.web;

import com.spexcrafters.taxonomy.domain.CategoryClassification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of {@code createCategory}. */
public record CreateCategoryRequest(
        @NotBlank @Size(max = 64) String code,
        String parentCode,
        @NotNull CategoryClassification classification,
        @NotBlank String originalLocale,
        @NotBlank @Size(max = 300) String name,
        Integer sortOrder) {
}
