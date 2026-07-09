package com.spexcrafters.supplier.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EvidenceFilenameSanitizationTest {

    @Test
    void stripsPathTraversalToBaseName() {
        assertThat(EvidenceService.sanitizeFilename("../../etc/passwd")).isEqualTo("passwd");
        assertThat(EvidenceService.sanitizeFilename("C:\\Windows\\system32\\cmd.exe")).isEqualTo("cmd.exe");
    }

    @Test
    void replacesUnsafeCharacters() {
        assertThat(EvidenceService.sanitizeFilename("in voice#1.pdf")).isEqualTo("in voice_1.pdf");
    }

    @Test
    void fallsBackForEmptyOrDotNames() {
        assertThat(EvidenceService.sanitizeFilename("")).isEqualTo("evidence");
        assertThat(EvidenceService.sanitizeFilename("..")).isEqualTo("evidence");
        assertThat(EvidenceService.sanitizeFilename(null)).isEqualTo("evidence");
    }

    @Test
    void preservesReasonableName() {
        assertThat(EvidenceService.sanitizeFilename("business-license_2026.pdf"))
                .isEqualTo("business-license_2026.pdf");
    }
}
