package com.spexcrafters.organizations.api;

import com.spexcrafters.organizations.domain.Capability;
import com.spexcrafters.organizations.domain.Organization;
import com.spexcrafters.organizations.domain.OrganizationMembership;
import com.spexcrafters.organizations.domain.OrganizationRole;
import java.util.Set;

/**
 * The resolved authorization context of an organization-scoped request: the organization,
 * the caller's ACTIVE membership and the capabilities derived from its role. Produced
 * exclusively by {@link OrganizationAccess}; consumed by the application services (never
 * by controllers — it carries JPA entities).
 */
public record OrganizationContext(
        Organization organization,
        OrganizationMembership membership,
        Set<Capability> capabilities) {

    public OrganizationRole role() {
        return membership.getRole();
    }

    public boolean has(Capability capability) {
        return capabilities.contains(capability);
    }
}
