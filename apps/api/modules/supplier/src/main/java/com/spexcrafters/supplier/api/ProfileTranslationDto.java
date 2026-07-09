package com.spexcrafters.supplier.api;

import com.spexcrafters.supplier.domain.TranslationSource;
import com.spexcrafters.supplier.domain.TranslationStatus;

/**
 * A per-locale supplier profile translation with its lifecycle metadata. {@code stale} is
 * derived (translation source version lags the profile's current source version); the client
 * shows a stale/MT/fallback indicator whenever the displayed language state is not APPROVED
 * and current.
 */
public record ProfileTranslationDto(
        String locale,
        boolean original,
        TranslationStatus translationStatus,
        TranslationSource translationSource,
        String sourceLocale,
        int sourceVersion,
        boolean stale,
        String tradingName,
        String companyDescription,
        String productionCapabilityDescription,
        String oemDescription,
        String odmDescription,
        String privateLabelDescription,
        String qualityControlDescription,
        String exportMarketDescription) {
}
