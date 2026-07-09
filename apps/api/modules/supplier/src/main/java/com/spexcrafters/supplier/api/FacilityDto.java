package com.spexcrafters.supplier.api;

import com.spexcrafters.supplier.domain.AddressPrivacy;
import com.spexcrafters.supplier.domain.FacilityOwnership;
import java.util.List;
import java.util.UUID;

/** A supplier facility with its localized name/description translations. */
public record FacilityDto(
        UUID id,
        String facilityTypeCode,
        String country,
        String region,
        String city,
        AddressPrivacy addressPrivacy,
        FacilityOwnership ownership,
        boolean isPublic,
        int sourceVersion,
        List<FacilityTranslationDto> translations) {
}
