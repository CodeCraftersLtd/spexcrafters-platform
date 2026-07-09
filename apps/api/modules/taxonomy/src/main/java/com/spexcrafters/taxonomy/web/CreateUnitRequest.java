package com.spexcrafters.taxonomy.web;

import com.spexcrafters.taxonomy.domain.UnitFamily;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Body of {@code createUnit}. */
public record CreateUnitRequest(
        @NotBlank @Size(max = 32) String code,
        @NotNull UnitFamily family,
        String baseUnitCode,
        BigDecimal factorToBase,
        BigDecimal offsetToBase,
        @Size(max = 64) String displayFormat,
        @NotBlank String originalLocale,
        @NotBlank @Size(max = 120) String displayName) {
}
