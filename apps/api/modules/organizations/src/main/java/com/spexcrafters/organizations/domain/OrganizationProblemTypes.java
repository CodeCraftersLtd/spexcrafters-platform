package com.spexcrafters.organizations.domain;

import java.net.URI;

/**
 * RFC 9457 problem {@code type} URIs owned by the organizations bounded context. Stable
 * identifiers referenced by the OpenAPI contract and frontend i18n mapping; not required
 * to resolve.
 */
public final class OrganizationProblemTypes {

    private static final String BASE = "https://api.spexcrafters.com/problems/";

    /** 404 — resource absent, or concealed from non-members by tenancy policy. */
    public static final URI NOT_FOUND = URI.create(BASE + "not-found");

    /** 403 — authenticated member lacks the required capability or violates a rank rule. */
    public static final URI AUTHORIZATION = URI.create(BASE + "authorization");

    /** 409 — the mutation would leave the organization without an ACTIVE OWNER. */
    public static final URI LAST_OWNER = URI.create(BASE + "last-owner");

    /** 409 — role change rejected by policy (e.g. self role-change). */
    public static final URI INVALID_ROLE_CHANGE = URI.create(BASE + "invalid-role-change");

    /** 409 — the target user already holds an ACTIVE membership in the organization. */
    public static final URI DUPLICATE_MEMBERSHIP = URI.create(BASE + "duplicate-membership");

    /** 403 — the invitation email does not match the authenticated account's email. */
    public static final URI INVITATION_IDENTITY_MISMATCH = URI.create(BASE + "invitation-identity-mismatch");

    private OrganizationProblemTypes() {
    }
}
