package com.spexcrafters.supplier.api;

import com.spexcrafters.supplier.domain.TranslationSource;

/**
 * The translatable class-D content of a supplier profile translation. {@code source}
 * distinguishes human vs machine translation (machine content is never shown as
 * human-verified). {@code null} fields are stored as absent.
 */
public record TranslationContent(
        TranslationSource source,
        String tradingName,
        String companyDescription,
        String productionCapabilityDescription,
        String oemDescription,
        String odmDescription,
        String privateLabelDescription,
        String qualityControlDescription,
        String exportMarketDescription) {
}
