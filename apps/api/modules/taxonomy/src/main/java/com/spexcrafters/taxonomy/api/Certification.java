package com.spexcrafters.taxonomy.api;

import com.spexcrafters.taxonomy.domain.CertificationCategory;
import java.util.UUID;

/** A certification registry row with its localized name/description (Certification schema). */
public record Certification(
        UUID id,
        String code,
        CertificationCategory category,
        String countryScope,
        String industryScope,
        Integer validityMonths,
        String name,
        String description,
        boolean deprecated) {
}
