package com.spexcrafters.taxonomy.api;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Pure ASCII SEO slug generation (ADR-027): lowercase, hyphen-separated, diacritics
 * transliterated, non-alphanumerics collapsed to single hyphens, trimmed, capped at 160 chars.
 * {@link #uniqueSlug} appends a numeric suffix on collision, keeping the result within the cap.
 */
public final class SlugGenerator {

    /** The DB {@code varchar(160)} cap on {@code category_slug.slug}. */
    public static final int MAX_LENGTH = 160;

    private SlugGenerator() {
    }

    /** Normalizes {@code input} to a base slug (never null; may be empty for punctuation-only input). */
    public static String slugify(String input) {
        if (input == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String ascii = decomposed
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        return truncate(ascii, MAX_LENGTH);
    }

    /**
     * A collision-free slug for {@code input}: the base slug, or that base with a {@code -2},
     * {@code -3}, … suffix until {@code taken} reports it free. Falls back to {@code item} for
     * empty base slugs (e.g. CJK-only names not yet transliterated).
     */
    public static String uniqueSlug(String input, Predicate<String> taken) {
        String base = slugify(input);
        if (base.isEmpty()) {
            base = "item";
        }
        if (!taken.test(base)) {
            return base;
        }
        for (int suffix = 2; ; suffix++) {
            String candidate = truncate(base, MAX_LENGTH - ("-" + suffix).length()) + "-" + suffix;
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
    }

    private static String truncate(String value, int max) {
        String trimmed = value.length() <= max ? value : value.substring(0, max);
        return trimmed.replaceAll("-+$", "");
    }
}
