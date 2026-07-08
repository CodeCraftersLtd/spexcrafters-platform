package com.spexcrafters.identity.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Token lifetimes. Defaults implement the contract: ~10-minute JWT access tokens and
 * 14-day rotating refresh tokens.
 */
@ConfigurationProperties(prefix = "spexcrafters.auth")
public record AuthProperties(
        @DefaultValue("10m") Duration accessTokenTtl,
        @DefaultValue("14d") Duration refreshTokenTtl,
        @DefaultValue("24h") Duration verificationTokenTtl) {
}
