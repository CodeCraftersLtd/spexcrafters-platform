package com.spexcrafters.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Overrides the application {@code Clock} bean with a {@link MutableClock} for tests that need
 * to advance time deterministically (e.g. proving the login-throttle window resets). Imported
 * only by the specific test class that needs it, so every other integration test keeps
 * {@code Clock.systemUTC()}.
 */
@TestConfiguration
public class MutableClockConfig {

    @Bean
    @Primary
    MutableClock testClock() {
        return MutableClock.startingNow();
    }
}
