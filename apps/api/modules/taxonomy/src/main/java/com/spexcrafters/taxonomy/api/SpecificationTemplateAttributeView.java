package com.spexcrafters.taxonomy.api;

import com.spexcrafters.taxonomy.domain.AttributeDataType;
import java.util.Map;

/**
 * One attribute slot of a category's effective specification template
 * (SpecificationTemplateAttributeView schema). {@code inherited} is true when the slot is
 * defined on an ancestor category; {@code sourceCategoryCode} names that category.
 */
public record SpecificationTemplateAttributeView(
        String attributeCode,
        AttributeDataType dataType,
        String name,
        String unitCode,
        String enumerationCode,
        boolean required,
        boolean inherited,
        String sourceCategoryCode,
        Map<String, Object> conditional,
        String defaultValue,
        int sortOrder) {
}
