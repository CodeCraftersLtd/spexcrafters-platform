package com.spexcrafters.taxonomy.web;

import com.spexcrafters.taxonomy.api.AttributeService.CreateAttributeInput;
import com.spexcrafters.taxonomy.domain.AttributeDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Body of {@code createAttribute}. */
public record CreateAttributeRequest(
        @NotBlank @Size(max = 64) String code,
        @NotNull AttributeDataType dataType,
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
        @NotBlank String originalLocale,
        @NotBlank @Size(max = 300) String name,
        @Size(max = 4000) String description) {

    public CreateAttributeInput toInput() {
        return new CreateAttributeInput(code, dataType, unitCode, enumerationCode, minValue, maxValue,
                minLength, maxLength, regexPattern, searchable, filterable, sortable, comparable, facetable,
                seo, visible, originalLocale, name, description);
    }
}
