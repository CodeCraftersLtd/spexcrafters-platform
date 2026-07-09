package com.spexcrafters.supplier.api;

import com.spexcrafters.supplier.domain.TranslationStatus;

/** A per-locale facility translation with lifecycle metadata and derived staleness. */
public record FacilityTranslationDto(
        String locale,
        boolean original,
        TranslationStatus translationStatus,
        int sourceVersion,
        boolean stale,
        String name,
        String description) {
}
