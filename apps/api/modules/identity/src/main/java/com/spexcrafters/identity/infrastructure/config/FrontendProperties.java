package com.spexcrafters.identity.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Where the Next.js frontend lives; used to build user-facing links such as the
 * email-verification URL.
 */
@ConfigurationProperties(prefix = "spexcrafters.web")
public record FrontendProperties(@DefaultValue("http://localhost:3000") String baseUrl) {
}
