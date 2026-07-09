package com.spexcrafters.taxonomy.web;

import com.spexcrafters.taxonomy.domain.TranslationSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of the taxonomy translation-upsert endpoints. {@code name} carries the label/display name. */
public record TranslationUpsertRequest(
        @NotBlank @Size(max = 300) String name,
        @Size(max = 4000) String description,
        @NotNull TranslationSource source) {
}
