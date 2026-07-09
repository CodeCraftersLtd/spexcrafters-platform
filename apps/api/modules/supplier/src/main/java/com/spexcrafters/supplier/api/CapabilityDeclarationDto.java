package com.spexcrafters.supplier.api;

import com.spexcrafters.supplier.domain.ClaimStatus;

/** A declared supplier capability (class-C code) and its claim status. */
public record CapabilityDeclarationDto(String capabilityCode, ClaimStatus claimStatus) {
}
