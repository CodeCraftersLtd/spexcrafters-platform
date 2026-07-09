package com.spexcrafters.taxonomy.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code addEnumerationValue}. */
public record AddEnumerationValueRequest(
        @NotBlank @Size(max = 64) String code,
        Integer sortOrder,
        @NotBlank String originalLocale,
        @NotBlank @Size(max = 300) String label,
        @Size(max = 2000) String description) {
}
