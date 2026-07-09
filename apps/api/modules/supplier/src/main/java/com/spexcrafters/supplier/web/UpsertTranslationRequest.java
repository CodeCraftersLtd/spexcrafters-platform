package com.spexcrafters.supplier.web;

import com.spexcrafters.supplier.api.TranslationContent;
import com.spexcrafters.supplier.domain.TranslationSource;
import jakarta.validation.constraints.Size;

/** Body of {@code upsertProfileTranslation}. All content fields are optional class-D text. */
public record UpsertTranslationRequest(
        TranslationSource source,
        @Size(max = 300) String tradingName,
        @Size(max = 4000) String companyDescription,
        @Size(max = 4000) String productionCapabilityDescription,
        @Size(max = 4000) String oemDescription,
        @Size(max = 4000) String odmDescription,
        @Size(max = 4000) String privateLabelDescription,
        @Size(max = 4000) String qualityControlDescription,
        @Size(max = 4000) String exportMarketDescription) {

    TranslationContent toContent() {
        return new TranslationContent(source, tradingName, companyDescription,
                productionCapabilityDescription, oemDescription, odmDescription, privateLabelDescription,
                qualityControlDescription, exportMarketDescription);
    }
}
