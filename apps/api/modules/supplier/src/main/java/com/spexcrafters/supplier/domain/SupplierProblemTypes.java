package com.spexcrafters.supplier.domain;

import java.net.URI;

/**
 * RFC 9457 problem {@code type} URIs owned by the supplier bounded context. Stable
 * identifiers referenced by the OpenAPI contract and frontend i18n mapping.
 */
public final class SupplierProblemTypes {

    private static final String BASE = "https://api.spexcrafters.com/problems/";

    /** 404 — resource absent, or concealed from non-members by tenancy policy. */
    public static final URI NOT_FOUND = URI.create(BASE + "not-found");

    /** 403 — authenticated org member lacks the required supplier capability. */
    public static final URI AUTHORIZATION = URI.create(BASE + "authorization");

    /** 409 — the requested application lifecycle transition is not allowed from the current state. */
    public static final URI INVALID_APPLICATION_STATE = URI.create(BASE + "invalid-application-state");

    /** 409 — the organization already has an active supplier identity. */
    public static final URI ONE_ACTIVE_SUPPLIER = URI.create(BASE + "one-active-supplier");

    /** 409 — the evidence cannot be acted on in its current state (e.g. not downloadable, retained). */
    public static final URI EVIDENCE_STATE = URI.create(BASE + "evidence-state");

    private SupplierProblemTypes() {
    }
}
