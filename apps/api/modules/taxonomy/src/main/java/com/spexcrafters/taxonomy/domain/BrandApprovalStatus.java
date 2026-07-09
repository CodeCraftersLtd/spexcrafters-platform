package com.spexcrafters.taxonomy.domain;

/** Brand approval lifecycle (domain-model §8). Mirrors {@code ck_brand_approval_status}. */
public enum BrandApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    DEPRECATED
}
