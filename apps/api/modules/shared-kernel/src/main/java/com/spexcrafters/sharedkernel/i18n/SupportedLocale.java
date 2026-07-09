package com.spexcrafters.sharedkernel.i18n;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The 20 launch locales (docs/architecture/supported-locales.md) — the single backend
 * source of truth for BCP 47 locale codes. Codes are language-independent identifiers;
 * display labels live in UI message resources, never here.
 *
 * <p>Normalization is deterministic and {@link Locale#ROOT}-based (never the Turkish
 * locale — dotless-ı would corrupt {@code i}). Unknown, legacy or {@code x-default} inputs
 * resolve to the canonical fallback {@code en} rather than throwing, except where an API
 * explicitly requires a supported code ({@link #require(String)}).
 */
public enum SupportedLocale {

    EN("en", false),
    ZH_CN("zh-CN", false),
    KO("ko", false),
    JA("ja", false),
    TH("th", false),
    VI("vi", false),
    FR("fr", false),
    ES("es", false),
    ID("id", false),
    MS("ms", false),
    HI("hi", false),
    RU("ru", false),
    BN("bn", false),
    DE("de", false),
    UR("ur", true),
    FIL("fil", false),
    FA("fa", true),
    PT("pt", false),
    AR("ar", true),
    TR("tr", false);

    /** The canonical fallback locale; every content and label resolution ends here. */
    public static final SupportedLocale FALLBACK = EN;

    private static final Map<String, SupportedLocale> BY_LOWER_CODE = buildIndex();

    private final String code;
    private final boolean rightToLeft;

    SupportedLocale(String code, boolean rightToLeft) {
        this.code = code;
        this.rightToLeft = rightToLeft;
    }

    /** The canonical BCP 47 code (e.g. {@code zh-CN}). */
    public String code() {
        return code;
    }

    public boolean isRightToLeft() {
        return rightToLeft;
    }

    /** The 20 canonical codes, in registry order. */
    public static List<String> codes() {
        return Arrays.stream(values()).map(SupportedLocale::code).toList();
    }

    /**
     * Resolves a raw locale string to a supported locale if it is one of the 20 codes
     * (case-insensitive) or a documented alias ({@code zh}/{@code zh-Hans} → {@code zh-CN}).
     * Unknown values yield {@link Optional#empty()} — callers decide between rejecting and
     * falling back.
     */
    public static Optional<SupportedLocale> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return switch (normalized) {
            case "zh", "zh-hans", "zh_cn" -> Optional.of(ZH_CN);
            default -> Optional.ofNullable(BY_LOWER_CODE.get(normalized));
        };
    }

    /**
     * Normalizes a raw string to a canonical supported code, deterministically falling back
     * to {@code en} for anything unknown (never throws — used for best-effort display locale
     * resolution such as the public profile foundation).
     */
    public static String normalizeOrFallback(String raw) {
        return parse(raw).orElse(FALLBACK).code();
    }

    /**
     * Requires a supported locale, returning its canonical code. Unknown input is a client
     * error (used where the caller asserts a specific translation locale, e.g. creating a
     * translation row).
     */
    public static SupportedLocale require(String raw) {
        return parse(raw).orElseThrow(() ->
                new IllegalArgumentException("Unsupported locale: " + raw));
    }

    private static Map<String, SupportedLocale> buildIndex() {
        Map<String, SupportedLocale> index = new LinkedHashMap<>();
        for (SupportedLocale locale : values()) {
            index.put(locale.code.toLowerCase(Locale.ROOT), locale);
        }
        return index;
    }
}
