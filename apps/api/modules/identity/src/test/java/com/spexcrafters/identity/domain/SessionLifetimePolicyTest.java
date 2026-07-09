package com.spexcrafters.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SessionLifetimePolicyTest {

    private static final Instant T0 = Instant.parse("2026-07-09T12:00:00Z");

    private final SessionLifetimePolicy policy =
            new SessionLifetimePolicy(Duration.ofSeconds(15), Duration.ofDays(30));

    @Nested
    class GraceWindow {

        @Test
        void reuseAtTheRotationInstantIsWithinGrace() {
            assertThat(policy.isWithinGrace(T0, T0)).isTrue();
        }

        @Test
        void reuseInsideTheWindowIsWithinGrace() {
            assertThat(policy.isWithinGrace(T0, T0.plusSeconds(14))).isTrue();
        }

        @Test
        void reuseExactlyAtTheWindowBoundaryIsStillWithinGrace() {
            assertThat(policy.isWithinGrace(T0, T0.plusSeconds(15))).isTrue();
        }

        @Test
        void reuseAfterTheWindowIsNotWithinGrace() {
            assertThat(policy.isWithinGrace(T0, T0.plusSeconds(15).plusMillis(1))).isFalse();
        }

        @Test
        void zeroGraceDisablesTheWindowForAnyLaterReuse() {
            SessionLifetimePolicy strict = new SessionLifetimePolicy(Duration.ZERO, Duration.ofDays(30));
            assertThat(strict.isWithinGrace(T0, T0)).isTrue();
            assertThat(strict.isWithinGrace(T0, T0.plusMillis(1))).isFalse();
        }
    }

    @Nested
    class AbsoluteLifetime {

        @Test
        void aFreshFamilyIsNotExpired() {
            assertThat(policy.isFamilyExpired(T0, T0)).isFalse();
        }

        @Test
        void aFamilyInsideTheCapIsNotExpired() {
            assertThat(policy.isFamilyExpired(T0, T0.plus(Duration.ofDays(29)))).isFalse();
        }

        @Test
        void aFamilyExactlyAtTheCapIsNotYetExpired() {
            assertThat(policy.isFamilyExpired(T0, T0.plus(Duration.ofDays(30)))).isFalse();
        }

        @Test
        void aFamilyOlderThanTheCapIsExpired() {
            assertThat(policy.isFamilyExpired(T0, T0.plus(Duration.ofDays(30)).plusMillis(1))).isTrue();
        }
    }

    @Nested
    class Validation {

        @Test
        void rejectsNegativeGrace() {
            assertThatIllegalArgumentException().isThrownBy(
                    () -> new SessionLifetimePolicy(Duration.ofSeconds(-1), Duration.ofDays(30)));
        }

        @Test
        void rejectsNonPositiveAbsoluteTtl() {
            assertThatIllegalArgumentException().isThrownBy(
                    () -> new SessionLifetimePolicy(Duration.ofSeconds(15), Duration.ZERO));
            assertThatIllegalArgumentException().isThrownBy(
                    () -> new SessionLifetimePolicy(Duration.ofSeconds(15), Duration.ofDays(-1)));
        }
    }
}
