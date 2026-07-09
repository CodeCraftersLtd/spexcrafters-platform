package com.spexcrafters.supplier.web;

import com.spexcrafters.supplier.domain.AddressPrivacy;
import com.spexcrafters.supplier.domain.FacilityOwnership;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of {@code addSupplierFacility}. */
public record AddFacilityRequest(
        @NotBlank @Size(max = 64) String facilityTypeCode,
        @NotBlank @Size(min = 2, max = 2) String country,
        @Size(max = 200) String region,
        @Size(max = 200) String city,
        @NotNull AddressPrivacy addressPrivacy,
        @NotNull FacilityOwnership ownership,
        boolean isPublic,
        @Size(max = 300) String name,
        @Size(max = 4000) String description) {
}
