package com.spexcrafters.taxonomy.api;

import com.spexcrafters.taxonomy.domain.BrandApprovalStatus;
import com.spexcrafters.taxonomy.domain.BrandType;
import java.util.List;
import java.util.UUID;

/** A single brand by its stable code (BrandDetail schema). */
public record BrandDetail(
        UUID id,
        String code,
        BrandType brandType,
        String canonicalName,
        String displayName,
        String ownerCompany,
        String manufacturer,
        String countryCode,
        String website,
        String logoStorageKey,
        BrandApprovalStatus approvalStatus,
        List<String> aliases,
        int version) {
}
