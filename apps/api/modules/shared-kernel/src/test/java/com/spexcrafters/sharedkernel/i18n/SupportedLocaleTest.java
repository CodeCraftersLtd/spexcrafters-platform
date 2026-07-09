package com.spexcrafters.sharedkernel.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class SupportedLocaleTest {

    @Test
    void parsesCanonicalCodesCaseInsensitively() {
        assertThat(SupportedLocale.parse("EN")).contains(SupportedLocale.EN);
        assertThat(SupportedLocale.parse("zh-cn")).contains(SupportedLocale.ZH_CN);
        assertThat(SupportedLocale.parse("AR")).contains(SupportedLocale.AR);
    }

    @Test
    void mapsDocumentedAliasesToZhCn() {
        assertThat(SupportedLocale.parse("zh")).contains(SupportedLocale.ZH_CN);
        assertThat(SupportedLocale.parse("zh-Hans")).contains(SupportedLocale.ZH_CN);
    }

    @Test
    void unknownCodesAreEmpty() {
        assertThat(SupportedLocale.parse("x-default")).isEmpty();
        assertThat(SupportedLocale.parse("kr")).isEmpty();
        assertThat(SupportedLocale.parse(null)).isEmpty();
        assertThat(SupportedLocale.parse("  ")).isEmpty();
    }

    @Test
    void normalizeOrFallbackIsDeterministicEnForUnknown() {
        assertThat(SupportedLocale.normalizeOrFallback("nope")).isEqualTo("en");
        assertThat(SupportedLocale.normalizeOrFallback(null)).isEqualTo("en");
        assertThat(SupportedLocale.normalizeOrFallback("FR")).isEqualTo("fr");
    }

    @Test
    void turkishDottedIedgeCaseNormalizesWithRootLocale() {
        // 'id' (Indonesian) must resolve regardless of the default JVM locale (Turkish-i trap).
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertThat(SupportedLocale.parse("ID")).contains(SupportedLocale.ID);
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void registryHasTwentyLocalesAndThreeRtl() {
        assertThat(SupportedLocale.codes()).hasSize(20);
        long rtl = java.util.Arrays.stream(SupportedLocale.values())
                .filter(SupportedLocale::isRightToLeft).count();
        assertThat(rtl).isEqualTo(3);
        assertThat(SupportedLocale.FALLBACK).isEqualTo(SupportedLocale.EN);
    }
}
