package com.spexcrafters.organizations.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Invitation token lifetime. Shares the {@code spexcrafters.auth} prefix with the identity
 * module's token TTLs but binds only its own key; the 7-day default implements
 * organizations-capability-model.md §4.
 */
@ConfigurationProperties(prefix = "spexcrafters.auth")
public record InvitationProperties(@DefaultValue("7d") Duration invitationTtl) {
}
