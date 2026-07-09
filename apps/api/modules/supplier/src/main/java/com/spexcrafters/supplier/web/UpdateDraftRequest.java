package com.spexcrafters.supplier.web;

import com.spexcrafters.supplier.api.SupplierApplicationService.DraftUpdate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Body of {@code updateSupplierApplicationDraft}. Partial: {@code null} leaves a field
 * unchanged; {@code version} carries the optimistic-locking value from the last read.
 */
public record UpdateDraftRequest(
        @Size(max = 300) String legalName,
        @Size(max = 300) String registeredLegalNameTranslated,
        @Size(max = 300) String tradingName,
        @Size(max = 120) String registrationNumber,
        @Size(max = 2) String countryOfRegistration,
        @Size(max = 300) String registrationAuthority,
        LocalDate registrationDate,
        @Size(max = 64) String companyTypeCode,
        Integer yearEstablished,
        @Size(max = 32) String employeeRange,
        @Size(max = 300) String website,
        @Size(max = 254) String businessEmail,
        @Size(max = 40) String businessPhone,
        List<String> types,
        List<String> capabilities,
        @NotNull Integer version) {

    DraftUpdate toDraftUpdate() {
        return new DraftUpdate(legalName, registeredLegalNameTranslated, tradingName, registrationNumber,
                countryOfRegistration, registrationAuthority, registrationDate, companyTypeCode,
                yearEstablished, employeeRange, website, businessEmail, businessPhone, types, capabilities);
    }
}
