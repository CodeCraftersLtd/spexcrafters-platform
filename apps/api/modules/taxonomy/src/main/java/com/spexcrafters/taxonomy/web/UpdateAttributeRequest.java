package com.spexcrafters.taxonomy.web;

import com.spexcrafters.taxonomy.api.AttributeService.UpdateAttributeInput;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Body of {@code updateAttribute}. */
public record UpdateAttributeRequest(
        String unitCode,
        String enumerationCode,
        BigDecimal minValue,
        BigDecimal maxValue,
        Integer minLength,
        Integer maxLength,
        @Size(max = 500) String regexPattern,
        boolean searchable,
        boolean filterable,
        boolean sortable,
        boolean comparable,
        boolean facetable,
        boolean seo,
        Boolean visible,
        @NotNull Integer version) {

    public UpdateAttributeInput toInput() {
        return new UpdateAttributeInput(unitCode, enumerationCode, minValue, maxValue, minLength, maxLength,
                regexPattern, searchable, filterable, sortable, comparable, facetable, seo, visible);
    }
}
