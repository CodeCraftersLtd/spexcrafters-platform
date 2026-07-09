package com.spexcrafters.taxonomy.web;

import com.spexcrafters.taxonomy.domain.CertificationCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code createCertification}. */
public record CreateCertificationRequest(
        @NotBlank @Size(max = 64) String code,
        CertificationCategory category,
        @Size(min = 2, max = 2) String countryScope,
        String industryScope,
        Integer validityMonths,
        @NotBlank String originalLocale,
        @NotBlank @Size(max = 300) String name,
        @Size(max = 4000) String description) {
}
