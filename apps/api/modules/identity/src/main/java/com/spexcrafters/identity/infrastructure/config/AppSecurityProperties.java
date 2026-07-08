package com.spexcrafters.identity.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Security settings.
 *
 * @param jwtSecret          HMAC-SHA256 signing secret for access tokens; must be at least
 *                           32 bytes (validated at startup by {@code JwtConfig})
 * @param corsAllowedOrigins browser origins allowed to call the API (comma-separated in env)
 */
@ConfigurationProperties(prefix = "spexcrafters.security")
public record AppSecurityProperties(
        String jwtSecret,
        @DefaultValue("http://localhost:3000") List<String> corsAllowedOrigins) {
}
