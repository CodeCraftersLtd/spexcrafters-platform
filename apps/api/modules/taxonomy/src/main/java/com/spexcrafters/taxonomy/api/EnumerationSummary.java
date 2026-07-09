package com.spexcrafters.taxonomy.api;

import java.util.UUID;

/** A reusable enumeration registry, summarized (EnumerationSummary schema). */
public record EnumerationSummary(
        UUID id,
        String code,
        boolean active,
        int valueCount) {
}
