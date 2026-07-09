package com.spexcrafters.taxonomy.api;

import java.util.List;

/** An enumeration with its values and localized labels (EnumerationDetail schema). */
public record EnumerationDetail(
        String code,
        boolean active,
        List<EnumerationValueView> values) {
}
