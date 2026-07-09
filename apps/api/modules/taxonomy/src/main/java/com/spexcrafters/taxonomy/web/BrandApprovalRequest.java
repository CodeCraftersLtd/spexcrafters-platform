package com.spexcrafters.taxonomy.web;

import com.spexcrafters.taxonomy.domain.BrandApprovalStatus;
import jakarta.validation.constraints.NotNull;

/** Body of {@code setBrandApproval}. */
public record BrandApprovalRequest(@NotNull BrandApprovalStatus status) {
}
