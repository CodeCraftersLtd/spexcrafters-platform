package com.spexcrafters.organizations.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Where the Next.js frontend lives; used to build the invitation-acceptance link. The
 * identity module binds the same {@code spexcrafters.web} prefix with its own private
 * properties class — module boundaries forbid sharing the type, not the configuration.
 */
@ConfigurationProperties(prefix = "spexcrafters.web")
public record OrganizationsFrontendProperties(@DefaultValue("http://localhost:3000") String baseUrl) {
}
