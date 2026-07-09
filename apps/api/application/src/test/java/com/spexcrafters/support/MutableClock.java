package com.spexcrafters.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A {@link Clock} whose instant can be advanced by a test, so time-window behavior
 * (e.g. the 15-minute login-throttle window) can be exercised deterministically without
 * real wall-clock waiting. Used only in integration tests via {@link MutableClockConfig};
 * production wires {@code Clock.systemUTC()}.
 */
public final class MutableClock extends Clock {

    private volatile Instant instant;
    private final ZoneId zone;

    public MutableClock(Instant initial, ZoneId zone) {
        this.instant = initial;
        this.zone = zone;
    }

    /** A clock starting at the real current instant, in UTC. */
    public static MutableClock startingNow() {
        return new MutableClock(Instant.now(), ZoneOffset.UTC);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        return new MutableClock(instant, newZone);
    }

    @Override
    public Instant instant() {
        return instant;
    }

    /** Moves the clock forward by {@code amount}. */
    public void advance(Duration amount) {
        this.instant = this.instant.plus(amount);
    }

    /** Resets the clock to a specific instant. */
    public void setInstant(Instant newInstant) {
        this.instant = newInstant;
    }
}
