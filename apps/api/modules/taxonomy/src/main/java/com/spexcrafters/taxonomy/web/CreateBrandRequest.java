package com.spexcrafters.taxonomy.web;

import com.spexcrafters.taxonomy.domain.BrandType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of {@code createBrand}. */
public record CreateBrandRequest(
        @NotBlank @Size(max = 64) String code,
        @NotNull BrandType brandType,
        @NotBlank @Size(max = 200) String canonicalName,
        @Size(max = 300) String ownerCompany,
        @Size(max = 300) String manufacturer,
        @Size(min = 2, max = 2) String countryCode,
        @Size(max = 300) String website,
        @NotBlank String originalLocale,
        @Size(max = 200) String displayName) {
}
