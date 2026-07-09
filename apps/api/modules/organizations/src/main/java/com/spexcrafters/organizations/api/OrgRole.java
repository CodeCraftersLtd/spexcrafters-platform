package com.spexcrafters.organizations.api;

/**
 * The organization role as exposed on the {@code api} surface for cross-module authorization
 * (the supplier context derives supplier capabilities from it). Mirrors the internal
 * {@code OrganizationRole} domain enum; kept in {@code api} so consumers never reach into
 * the organizations domain.
 */
public enum OrgRole {
    OWNER,
    ADMIN,
    MEMBER
}
