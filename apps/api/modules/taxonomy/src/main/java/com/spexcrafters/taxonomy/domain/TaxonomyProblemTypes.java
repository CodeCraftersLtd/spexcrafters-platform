package com.spexcrafters.taxonomy.domain;

import java.net.URI;

/**
 * RFC 9457 problem {@code type} URIs owned by the taxonomy bounded context. Stable
 * identifiers referenced by the OpenAPI contract and frontend i18n mapping.
 */
public final class TaxonomyProblemTypes {

    private static final String BASE = "https://api.spexcrafters.com/problems/";

    /** 404 — the requested registry entry is absent. */
    public static final URI NOT_FOUND = URI.create(BASE + "not-found");

    /** 409 — a stable code / slug already exists, or an optimistic-lock version mismatch. */
    public static final URI CONFLICT = URI.create(BASE + "conflict");

    private TaxonomyProblemTypes() {
    }
}
