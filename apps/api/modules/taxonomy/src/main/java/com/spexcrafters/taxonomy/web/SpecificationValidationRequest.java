package com.spexcrafters.taxonomy.web;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/** Body of {@code validateSpecification}: a category code and a map of attribute values. */
public record SpecificationValidationRequest(
        @NotBlank String categoryCode,
        Map<String, String> values) {
}
