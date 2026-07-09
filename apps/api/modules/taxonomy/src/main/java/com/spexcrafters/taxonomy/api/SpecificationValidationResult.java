package com.spexcrafters.taxonomy.api;

import java.util.List;

/** The result of validating attribute values against a category template. */
public record SpecificationValidationResult(
        boolean valid,
        List<SpecificationViolation> violations) {

    public static SpecificationValidationResult of(List<SpecificationViolation> violations) {
        return new SpecificationValidationResult(violations.isEmpty(), violations);
    }
}
