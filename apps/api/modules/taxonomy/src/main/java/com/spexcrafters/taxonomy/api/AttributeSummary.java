package com.spexcrafters.taxonomy.api;

import com.spexcrafters.taxonomy.domain.AttributeDataType;

/** A row of the master attribute registry (AttributeSummary schema). */
public record AttributeSummary(
        String code,
        AttributeDataType dataType,
        String name,
        String unitCode,
        String enumerationCode,
        boolean deprecated,
        boolean visible,
        boolean searchable,
        boolean filterable,
        boolean sortable,
        boolean comparable,
        boolean facetable,
        boolean seo) {
}
