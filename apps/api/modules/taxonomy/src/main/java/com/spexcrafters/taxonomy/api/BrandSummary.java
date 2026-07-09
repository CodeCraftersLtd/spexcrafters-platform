package com.spexcrafters.taxonomy.api;

import com.spexcrafters.taxonomy.domain.BrandApprovalStatus;
import com.spexcrafters.taxonomy.domain.BrandType;
import java.util.UUID;

/** A public brand, summarized (BrandSummary schema). */
public record BrandSummary(
        UUID id,
        String code,
        BrandType brandType,
        String canonicalName,
        String displayName,
        String countryCode,
        BrandApprovalStatus approvalStatus) {
}
