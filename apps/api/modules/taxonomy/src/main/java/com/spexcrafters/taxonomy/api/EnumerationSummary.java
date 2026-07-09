package com.spexcrafters.taxonomy.api;

/** A reusable enumeration registry, summarized (EnumerationSummary schema). */
public record EnumerationSummary(
        String code,
        boolean active,
        int valueCount) {
}
