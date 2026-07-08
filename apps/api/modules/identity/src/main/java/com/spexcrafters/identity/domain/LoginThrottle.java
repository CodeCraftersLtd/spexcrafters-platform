package com.spexcrafters.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;

/**
 * Brute-force window logic: more than {@value #MAX_FAILURES} credential failures for one
 * email within {@link #WINDOW} blocks further attempts until the earliest failure ages out.
 * Pure function of its inputs so it is trivially unit-testable; the caller supplies the
 * failure count and earliest failure from the {@code login_attempt} table.
 */
public final class LoginThrottle {

    public static final int MAX_FAILURES = 5;
    public static final Duration WINDOW = Duration.ofMinutes(15);

    private LoginThrottle() {
    }

    /**
     * @param recentFailureCount number of {@code FAILURE} attempts within the window
     * @param earliestFailureAt  timestamp of the earliest failure inside the window
     * @param now                current instant
     * @return empty when the attempt may proceed; otherwise the {@code Retry-After} seconds
     */
    public static OptionalLong retryAfterSeconds(long recentFailureCount, Instant earliestFailureAt, Instant now) {
        if (recentFailureCount <= MAX_FAILURES) {
            return OptionalLong.empty();
        }
        long seconds = Duration.between(now, earliestFailureAt.plus(WINDOW)).toSeconds();
        return OptionalLong.of(Math.max(seconds, 1L));
    }
}
