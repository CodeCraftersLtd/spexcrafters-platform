package com.spexcrafters.sharedkernel.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogSanitizerTest {

    @Test
    void masksTheLocalPartKeepingItsFirstCharacterAndTheDomain() {
        assertThat(LogSanitizer.maskEmail("verity@example.com")).isEqualTo("v***@example.com");
    }

    @Test
    void aSingleCharacterLocalPartStillMasks() {
        assertThat(LogSanitizer.maskEmail("v@example.com")).isEqualTo("v***@example.com");
    }

    @Test
    void neverEchoesTheRemainderOfTheLocalPart() {
        assertThat(LogSanitizer.maskEmail("firstname.lastname@example.com"))
                .isEqualTo("f***@example.com")
                .doesNotContain("irstname");
    }

    @Test
    void anEmptyLocalPartCollapsesToTheMask() {
        assertThat(LogSanitizer.maskEmail("@example.com")).isEqualTo("***@example.com");
    }

    @Test
    void aValueWithoutAnAtSignCollapsesToTheMask() {
        assertThat(LogSanitizer.maskEmail("not-an-email")).isEqualTo("***");
    }

    @Test
    void nullAndBlankCollapseToTheMaskInsteadOfThrowing() {
        assertThat(LogSanitizer.maskEmail(null)).isEqualTo("***");
        assertThat(LogSanitizer.maskEmail("  ")).isEqualTo("***");
    }
}
