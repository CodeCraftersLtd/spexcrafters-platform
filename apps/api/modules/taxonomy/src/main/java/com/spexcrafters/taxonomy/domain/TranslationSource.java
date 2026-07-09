package com.spexcrafters.taxonomy.domain;

/** Provenance of a translation row (ADR-020), cloned into the taxonomy bounded context. */
public enum TranslationSource {
    HUMAN,
    MACHINE,
    IMPORT
}
