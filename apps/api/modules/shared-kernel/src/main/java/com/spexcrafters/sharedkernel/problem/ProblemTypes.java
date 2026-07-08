package com.spexcrafters.sharedkernel.problem;

import java.net.URI;

/**
 * RFC 9457 problem {@code type} URIs used across the platform. The URIs are stable
 * identifiers referenced by the OpenAPI contract and frontend i18n mapping; they are not
 * required to resolve.
 */
public final class ProblemTypes {

    private static final String BASE = "https://api.spexcrafters.com/problems/";

    public static final URI VALIDATION_ERROR = URI.create(BASE + "validation-error");
    public static final URI CONFLICT = URI.create(BASE + "conflict");
    public static final URI AUTHENTICATION_FAILED = URI.create(BASE + "authentication-failed");
    public static final URI CONCURRENT_REFRESH = URI.create(BASE + "concurrent-refresh");
    public static final URI FORBIDDEN = URI.create(BASE + "forbidden");
    public static final URI EMAIL_NOT_VERIFIED = URI.create(BASE + "email-not-verified");
    public static final URI TOKEN_GONE = URI.create(BASE + "token-gone");
    public static final URI RATE_LIMITED = URI.create(BASE + "rate-limited");
    public static final URI INTERNAL_ERROR = URI.create(BASE + "internal-error");

    private ProblemTypes() {
    }
}
