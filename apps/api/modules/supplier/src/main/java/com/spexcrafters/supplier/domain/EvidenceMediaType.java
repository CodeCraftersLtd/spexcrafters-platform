package com.spexcrafters.supplier.domain;

import java.util.Optional;

/**
 * The allowlist of evidence content types accepted in Phase 7 (images + PDF only) and their
 * magic-byte signatures. Finalize validates the actual leading bytes of the stored object
 * against the declared type, rejecting spoofed content types — the extension/declared type is
 * never trusted.
 */
public enum EvidenceMediaType {

    PDF("application/pdf", new byte[] {0x25, 0x50, 0x44, 0x46}),        // %PDF
    JPEG("image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("image/png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
    // WEBP is RIFF-container: "RIFF" at 0 and "WEBP" at 8 (checked specially in detect()).
    WEBP("image/webp", new byte[] {0x52, 0x49, 0x46, 0x46});

    private final String mediaType;
    private final byte[] signature;

    EvidenceMediaType(String mediaType, byte[] signature) {
        this.mediaType = mediaType;
        this.signature = signature;
    }

    public String mediaType() {
        return mediaType;
    }

    /** The allowed type whose declared media type matches, if any. */
    public static Optional<EvidenceMediaType> fromDeclared(String declaredMediaType) {
        if (declaredMediaType == null) {
            return Optional.empty();
        }
        String normalized = declaredMediaType.trim().toLowerCase(java.util.Locale.ROOT);
        for (EvidenceMediaType type : values()) {
            if (type.mediaType.equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /** The allowed type whose magic bytes match the object's leading bytes, if any. */
    public static Optional<EvidenceMediaType> detect(byte[] content) {
        if (isWebp(content)) {
            return Optional.of(WEBP);
        }
        for (EvidenceMediaType type : values()) {
            if (type != WEBP && startsWith(content, type.signature)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    private static boolean isWebp(byte[] content) {
        return content != null && content.length >= 12
                && startsWith(content, WEBP.signature)
                && content[8] == 0x57 && content[9] == 0x45 && content[10] == 0x42 && content[11] == 0x50;
    }

    private static boolean startsWith(byte[] content, byte[] prefix) {
        if (content == null || content.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (content[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
