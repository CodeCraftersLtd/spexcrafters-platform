package com.spexcrafters.taxonomy.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.spexcrafters.taxonomy.domain.CategoryTranslation;
import com.spexcrafters.taxonomy.domain.TranslationSource;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure tests of the ADR-020 fallback chain and stale detection. */
class LocalizationResolverTest {

    private static CategoryTranslation row(String locale, boolean original, int sourceVersion, String name) {
        CategoryTranslation t = new CategoryTranslation(UUID.randomUUID(), UUID.randomUUID(), locale,
                original ? locale : "en", sourceVersion, TranslationSource.HUMAN, original, null);
        t.applyContent(name, null, sourceVersion, TranslationSource.HUMAN, null);
        if (!original) {
            t.approve(UUID.randomUUID(), java.time.Instant.now());
        }
        return t;
    }

    @Test
    void prefersRequestedApprovedLocale() {
        var resolved = LocalizationResolver.resolve(
                List.of(row("en", true, 1, "Frame"), row("fr", false, 1, "Monture")), "fr", 1);
        assertThat(resolved.displayLocale()).isEqualTo("fr");
        assertThat(resolved.fallbackApplied()).isFalse();
        assertThat(resolved.translation().getName()).isEqualTo("Monture");
        assertThat(resolved.stale()).isFalse();
    }

    @Test
    void fallsBackToOriginalWhenRequestedMissing() {
        var resolved = LocalizationResolver.resolve(List.of(row("en", true, 1, "Frame")), "de", 1);
        assertThat(resolved.displayLocale()).isEqualTo("en");
        assertThat(resolved.fallbackApplied()).isTrue();
        assertThat(resolved.translation().getName()).isEqualTo("Frame");
    }

    @Test
    void fallsBackToEnglishWhenNeitherRequestedNorOriginalMatch() {
        var resolved = LocalizationResolver.resolve(
                List.of(row("zh-CN", true, 1, "镜架"), row("en", false, 1, "Frame")), "fr", 1);
        assertThat(resolved.displayLocale()).isEqualTo("zh-CN");
        assertThat(resolved.translation().getName()).isEqualTo("镜架");
    }

    @Test
    void marksNonOriginalTranslationStaleWhenSourceMovedOn() {
        var resolved = LocalizationResolver.resolve(
                List.of(row("en", true, 2, "Frame"), row("fr", false, 1, "Monture")), "fr", 2);
        assertThat(resolved.displayLocale()).isEqualTo("fr");
        assertThat(resolved.stale()).isTrue();
    }

    @Test
    void untranslatedWhenNothingDisplayable() {
        var resolved = LocalizationResolver.resolve(List.<CategoryTranslation>of(), "en", 1);
        assertThat(resolved.isPresent()).isFalse();
        assertThat(resolved.fallbackApplied()).isTrue();
    }
}
