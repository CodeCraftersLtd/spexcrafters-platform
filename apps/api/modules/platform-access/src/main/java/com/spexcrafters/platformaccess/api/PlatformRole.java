package com.spexcrafters.platformaccess.api;

/**
 * Platform-staff roles (supplier-domain-model §6). Entirely separate from organization
 * roles: platform staff moderate suppliers across all tenants and an organization OWNER can
 * never hold a platform role. Rank order {@code PLATFORM_ADMIN > SENIOR_REVIEWER > REVIEWER}.
 *
 * <p>Part of the public {@code api} surface because it is the cross-module authorization
 * vocabulary consumed by the supplier and verification contexts.
 */
public enum PlatformRole {
    REVIEWER,
    SENIOR_REVIEWER,
    PLATFORM_ADMIN
}
