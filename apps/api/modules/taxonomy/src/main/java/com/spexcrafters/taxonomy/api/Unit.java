package com.spexcrafters.taxonomy.api;

import com.spexcrafters.taxonomy.domain.UnitFamily;
import java.math.BigDecimal;

/** A unit-of-measure registry row with its localized display name (Unit schema). */
public record Unit(
        String code,
        UnitFamily family,
        String baseUnitCode,
        BigDecimal factorToBase,
        BigDecimal offsetToBase,
        String displayName,
        String displayFormat) {
}
