package com.spexcrafters.taxonomy.api;

import com.spexcrafters.taxonomy.domain.AttributeDataType;
import com.spexcrafters.taxonomy.domain.TranslationStatus;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** A single attribute by its stable code (AttributeDetail schema). */
public record AttributeDetail(
        UUID id,
        String code,
        AttributeDataType dataType,
        String unitCode,
        String enumerationCode,
        BigDecimal minValue,
        BigDecimal maxValue,
        Integer minLength,
        Integer maxLength,
        String regexPattern,
        boolean searchable,
        boolean filterable,
        boolean sortable,
        boolean comparable,
        boolean facetable,
        boolean seo,
        boolean visible,
        boolean deprecated,
        int sortOrder,
        String name,
        String description,
        Map<String, TranslationStatus> translationStatuses,
        int version) {
}
