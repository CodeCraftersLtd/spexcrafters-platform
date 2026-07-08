package com.spexcrafters.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class LoginThrottleTest {

    private static final Instant NOW = Instant.parse("2026-07-08T12:00:00Z");

    @Test
    void allowsUpToFiveRecentFailures() {
        for (long failures = 0; failures <= 5; failures++) {
            assertThat(LoginThrottle.retryAfterSeconds(failures, NOW.minusSeconds(60), NOW))
                    .as("%d failures should not throttle", failures)
                    .isEmpty();
        }
    }

    @Test
    void throttlesOnMoreThanFiveFailures() {
        OptionalLong retryAfter = LoginThrottle.retryAfterSeconds(6, NOW.minusSeconds(60), NOW);

        assertThat(retryAfter).isPresent();
    }

    @Test
    void retryAfterCountsDownToWhenTheEarliestFailureLeavesTheWindow() {
        // Earliest failure 5 minutes ago; the 15-minute window clears in 10 minutes.
        Instant earliestFailure = NOW.minusSeconds(300);

        OptionalLong retryAfter = LoginThrottle.retryAfterSeconds(6, earliestFailure, NOW);

        assertThat(retryAfter).hasValue(600L);
    }

    @Test
    void retryAfterIsAtLeastOneSecond() {
        // Earliest failure is about to age out; never advertise a zero/negative wait.
        Instant earliestFailure = NOW.minus(LoginThrottle.WINDOW);

        OptionalLong retryAfter = LoginThrottle.retryAfterSeconds(10, earliestFailure, NOW);

        assertThat(retryAfter).hasValue(1L);
    }
}
