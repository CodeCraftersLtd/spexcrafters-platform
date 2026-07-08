package com.spexcrafters.identity.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Token lifetimes. Defaults implement the contract: ~10-minute JWT access tokens,
 * 14-day rotating refresh tokens, a 15-second concurrency grace window for rotated
 * refresh tokens and a 30-day absolute session lifetime per token family
 * (docs/security/session-security-policy.md §§1–2).
 */
@ConfigurationProperties(prefix = "spexcrafters.auth")
public record AuthProperties(
        @DefaultValue("10m") Duration accessTokenTtl,
        @DefaultValue("14d") Duration refreshTokenTtl,
        @DefaultValue("24h") Duration verificationTokenTtl,
        @DefaultValue("15s") Duration refreshGrace,
        @DefaultValue("30d") Duration sessionAbsoluteTtl) {
}
