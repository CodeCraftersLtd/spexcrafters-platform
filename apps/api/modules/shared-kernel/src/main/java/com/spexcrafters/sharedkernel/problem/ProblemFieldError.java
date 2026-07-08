package com.spexcrafters.sharedkernel.problem;

/**
 * One entry of the {@code errors[]} array in a problem+json response, matching the
 * {@code Problem} schema of the OpenAPI contract. {@code code} is a stable machine-readable
 * key the frontend maps to i18n messages; {@code message} is a human-readable fallback.
 */
public record ProblemFieldError(String field, String code, String message) {
}
