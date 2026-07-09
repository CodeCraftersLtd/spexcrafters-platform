package com.spexcrafters.taxonomy.api;

import com.spexcrafters.taxonomy.domain.TranslationStatus;

/** A per-locale translation view returned by translation upsert/approve (TranslationView schema). */
public record TranslationView(
        String locale,
        String name,
        String description,
        TranslationStatus translationStatus,
        boolean isOriginal,
        boolean stale,
        int sourceVersion) {
}
