package com.spexcrafters.sharedkernel.util;

/**
 * Masks personal data before it reaches log output (TD-4). Log statements must never
 * print a full email address; use {@link #maskEmail(String)} instead.
 */
public final class LogSanitizer {

    private static final String MASK = "***";

    private LogSanitizer() {
    }

    /**
     * Masks an email address for logging: {@code verity@example.com → v***@example.com}.
     * The domain is retained (operationally useful, not identifying on its own); the local
     * part is reduced to its first character. Degenerate values (null, blank, no {@code @},
     * empty local part) collapse to {@code ***} variants rather than throwing — a log
     * statement must never fail.
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return MASK;
        }
        int at = email.indexOf('@');
        if (at < 0) {
            return MASK;
        }
        String domain = email.substring(at + 1);
        if (at == 0) {
            return MASK + "@" + domain;
        }
        return email.charAt(0) + MASK + "@" + domain;
    }
}
