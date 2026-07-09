package com.spexcrafters.supplier.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EvidenceMediaTypeTest {

    @Test
    void detectsPdfByMagicBytes() {
        byte[] pdf = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37};
        assertThat(EvidenceMediaType.detect(pdf)).contains(EvidenceMediaType.PDF);
    }

    @Test
    void detectsPngByMagicBytes() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        assertThat(EvidenceMediaType.detect(png)).contains(EvidenceMediaType.PNG);
    }

    @Test
    void detectsWebpRiffContainer() {
        byte[] webp = {0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00,
                0x57, 0x45, 0x42, 0x50, 0x00};
        assertThat(EvidenceMediaType.detect(webp)).contains(EvidenceMediaType.WEBP);
    }

    @Test
    void doesNotMisdetectArbitraryRiffAsWebp() {
        byte[] wav = {0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00,
                0x57, 0x41, 0x56, 0x45, 0x00};
        assertThat(EvidenceMediaType.detect(wav)).isEmpty();
    }

    @Test
    void rejectsSpoofedContentType() {
        // Declared PDF but the bytes are a PNG — detection must not match the declared type.
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertThat(EvidenceMediaType.detect(png)).contains(EvidenceMediaType.PNG);
        assertThat(EvidenceMediaType.fromDeclared("application/pdf")).contains(EvidenceMediaType.PDF);
    }

    @Test
    void disallowedDeclaredTypeIsEmpty() {
        assertThat(EvidenceMediaType.fromDeclared("application/zip")).isEmpty();
        assertThat(EvidenceMediaType.fromDeclared(null)).isEmpty();
    }
}
