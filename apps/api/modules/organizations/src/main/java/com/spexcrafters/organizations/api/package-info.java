/**
 * Public API of the organizations bounded context: application services (organization
 * lifecycle, membership, invitations), the {@code OrganizationAccess} authorization
 * policy and the DTO records of the OpenAPI contract. Cross-module access is only
 * permitted through this {@code ...api} package (enforced by ArchUnit).
 */
package com.spexcrafters.organizations.api;
