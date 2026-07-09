package com.spexcrafters.supplier.api;

/**
 * A supported locale entry for {@code GET /locales}: the canonical BCP 47 code, text
 * direction and the deterministic fallback ({@code en} for all but {@code en} itself).
 */
public record LocaleDto(String code, String dir, String fallback) {
}
