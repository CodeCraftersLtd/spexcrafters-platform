package com.spexcrafters.taxonomy.api;

/**
 * A single specification-validation violation. {@code code} is a stable slug mapping to a
 * frontend i18n key (e.g. {@code required}, {@code out-of-range}, {@code not-a-member}).
 */
public record SpecificationViolation(
        String attributeCode,
        String code,
        String message) {
}
