package com.spexcrafters.taxonomy.api;

import java.util.List;
import java.util.UUID;

/** An enumeration with its values and localized labels (EnumerationDetail schema). */
public record EnumerationDetail(
        UUID id,
        String code,
        boolean active,
        List<EnumerationValueView> values) {
}
