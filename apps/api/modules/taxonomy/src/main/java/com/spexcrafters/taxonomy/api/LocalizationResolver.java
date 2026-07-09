package com.spexcrafters.taxonomy.api;

import com.spexcrafters.sharedkernel.i18n.SupportedLocale;
import com.spexcrafters.taxonomy.domain.TaxonomyTranslation;
import java.util.List;
import java.util.Optional;

/**
 * The ADR-020 localization fallback chain, shared by every taxonomy read: requested-locale
 * APPROVED/original → entity original locale → {@code en} APPROVED → untranslated. Only
 * {@code isOriginal} or {@code APPROVED} rows are ever displayed publicly. Pure and stateless.
 */
public final class LocalizationResolver {

    private LocalizationResolver() {
    }

    /** The resolved translation (may be null when nothing is displayable) and its language state. */
    public record Resolved<T extends TaxonomyTranslation>(
            T translation, String displayLocale, boolean fallbackApplied, boolean stale) {

        public boolean isPresent() {
            return translation != null;
        }
    }

    public static <T extends TaxonomyTranslation> Resolved<T> resolve(
            List<T> translations, String requestedRaw, int currentSourceVersion) {
        String requested = SupportedLocale.normalizeOrFallback(requestedRaw);
        String fallback = SupportedLocale.FALLBACK.code();

        Optional<T> original = translations.stream().filter(TaxonomyTranslation::isOriginal).findFirst();
        String originalLocale = original.map(TaxonomyTranslation::getLocale).orElse(fallback);

        // 1. requested-locale displayable row.
        Optional<T> requestedRow = byLocale(translations, requested);
        if (requestedRow.isPresent() && requestedRow.get().isDisplayable()) {
            T row = requestedRow.get();
            return new Resolved<>(row, requested, false, row.isStale(currentSourceVersion));
        }
        // 2. entity original locale (authoritative — never stale).
        if (!requested.equals(originalLocale) && original.isPresent()) {
            return new Resolved<>(original.get(), originalLocale, true, false);
        }
        // 3. en APPROVED.
        if (!requested.equals(fallback) && !originalLocale.equals(fallback)) {
            Optional<T> enRow = byLocale(translations, fallback);
            if (enRow.isPresent() && enRow.get().isDisplayable()) {
                T row = enRow.get();
                return new Resolved<>(row, fallback, true, row.isStale(currentSourceVersion));
            }
        }
        // 4. untranslated.
        return new Resolved<>(null, requested, true, false);
    }

    private static <T extends TaxonomyTranslation> Optional<T> byLocale(List<T> translations, String locale) {
        return translations.stream().filter(t -> t.getLocale().equals(locale)).findFirst();
    }
}
