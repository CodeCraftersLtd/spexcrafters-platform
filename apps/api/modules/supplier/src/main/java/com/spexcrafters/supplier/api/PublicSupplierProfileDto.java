package com.spexcrafters.supplier.api;

import java.util.UUID;

/**
 * The public, localized supplier profile foundation. Content resolves by the ADR-020
 * fallback chain (requested-locale APPROVED translation → supplier original language →
 * {@code en} APPROVED → untranslated). {@code displayLocale}/{@code fallbackApplied}/
 * {@code stale} let the public UI label the language state. Class-E fields (legal name,
 * registration number, country) are rendered as-is and never machine-translated.
 */
public record PublicSupplierProfileDto(
        UUID supplierId,
        String requestedLocale,
        String displayLocale,
        boolean fallbackApplied,
        boolean stale,
        String legalName,
        String tradingName,
        String companyDescription,
        String countryOfRegistration) {
}
