package com.spexcrafters.platformaccess.domain;

import java.net.URI;

/**
 * RFC 9457 problem {@code type} URIs owned by the platform-access context. Stable
 * identifiers referenced by the OpenAPI contract and frontend i18n mapping.
 */
public final class PlatformProblemTypes {

    private static final String BASE = "https://api.spexcrafters.com/problems/";

    /** 403 — the caller is not active platform staff, or lacks the required capability. */
    public static final URI AUTHORIZATION = URI.create(BASE + "authorization");

    private PlatformProblemTypes() {
    }
}
