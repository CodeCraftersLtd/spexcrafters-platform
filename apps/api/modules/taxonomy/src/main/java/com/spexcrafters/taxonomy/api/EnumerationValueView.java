package com.spexcrafters.taxonomy.api;

import java.util.UUID;

/** A single enumeration value with its localized label (EnumerationValueView schema). */
public record EnumerationValueView(
        UUID id,
        String code,
        String label,
        int sortOrder,
        boolean deprecated,
        boolean active) {
}
