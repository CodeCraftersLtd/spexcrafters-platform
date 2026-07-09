package com.spexcrafters.supplier.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The full supplier profile view: non-translatable (class-E) fields, declared types and
 * capabilities, per-locale translations and facilities. {@code sourceVersion} lets clients
 * reason about translation staleness.
 */
public record SupplierProfileDto(
        UUID supplierId,
        String originalLocale,
        String legalName,
        String registeredLegalNameTranslated,
        String tradingName,
        String registrationNumber,
        String countryOfRegistration,
        String registrationAuthority,
        LocalDate registrationDate,
        String companyTypeCode,
        Integer yearEstablished,
        String employeeRange,
        String website,
        String businessEmail,
        String businessPhone,
        int sourceVersion,
        List<String> types,
        List<CapabilityDeclarationDto> capabilities,
        List<FacilityDto> facilities,
        List<ProfileTranslationDto> translations) {
}
