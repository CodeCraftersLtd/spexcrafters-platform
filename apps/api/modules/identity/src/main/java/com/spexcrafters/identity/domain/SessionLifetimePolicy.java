package com.spexcrafters.identity.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * Pure clock arithmetic of the Phase-6 session hardening rules
 * (docs/security/session-security-policy.md §§1–2):
 *
 * <ul>
 *   <li><b>Concurrency grace window:</b> presenting an already-rotated refresh token within
 *       {@code refreshGrace} of its rotation is a benign multi-tab race, not theft — the
 *       request fails (401) but the family survives. Reuse after the window is a replay.</li>
 *   <li><b>Absolute session lifetime:</b> a token family older than
 *       {@code sessionAbsoluteTtl} (measured from the family's first token, i.e. login)
 *       can no longer be renewed regardless of activity.</li>
 * </ul>
 */
public record SessionLifetimePolicy(Duration refreshGrace, Duration sessionAbsoluteTtl) {

    public SessionLifetimePolicy {
        if (refreshGrace.isNegative()) {
            throw new IllegalArgumentException("refreshGrace must not be negative");
        }
        if (sessionAbsoluteTtl.isNegative() || sessionAbsoluteTtl.isZero()) {
            throw new IllegalArgumentException("sessionAbsoluteTtl must be positive");
        }
    }

    /** True while {@code now} is at most {@code refreshGrace} after the rotation instant. */
    public boolean isWithinGrace(Instant rotatedAt, Instant now) {
        return !now.isAfter(rotatedAt.plus(refreshGrace));
    }

    /** True once the family (created at login) has outlived the absolute session lifetime. */
    public boolean isFamilyExpired(Instant familyCreatedAt, Instant now) {
        return now.isAfter(familyCreatedAt.plus(sessionAbsoluteTtl));
    }
}
