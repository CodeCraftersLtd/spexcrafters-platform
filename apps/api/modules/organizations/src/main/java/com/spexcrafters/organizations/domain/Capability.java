package com.spexcrafters.organizations.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Set;

/**
 * Closed, typed capability set of the organizations bounded context. Authorization is
 * decided against this enum — never scattered string comparisons. Serialized on the wire
 * with the dotted names of the OpenAPI {@code Capability} schema.
 *
 * <p>{@code organization.delete} does not exist in this slice by design (deletion policy
 * arrives with the compliance workstream).
 */
public enum Capability {
    ORGANIZATION_READ("organization.read"),
    ORGANIZATION_UPDATE("organization.update"),
    MEMBERS_READ("organization.members.read"),
    MEMBERS_INVITE("organization.members.invite"),
    MEMBERS_REMOVE("organization.members.remove"),
    ROLES_MANAGE("organization.roles.manage");

    private final String wireName;

    Capability(String wireName) {
        this.wireName = wireName;
    }

    @JsonValue
    public String wireName() {
        return wireName;
    }

    /**
     * The role→capability matrix of organizations-capability-model.md §2. The returned set
     * is immutable. Note that the matrix alone is not the full policy: rank rules (ADMIN
     * may only invite/remove MEMBER-role targets) are enforced by the application services
     * on top of these grants.
     */
    public static Set<Capability> forRole(OrganizationRole role) {
        return switch (role) {
            case OWNER -> Set.of(ORGANIZATION_READ, ORGANIZATION_UPDATE, MEMBERS_READ,
                    MEMBERS_INVITE, MEMBERS_REMOVE, ROLES_MANAGE);
            case ADMIN -> Set.of(ORGANIZATION_READ, ORGANIZATION_UPDATE, MEMBERS_READ,
                    MEMBERS_INVITE, MEMBERS_REMOVE);
            case MEMBER -> Set.of(ORGANIZATION_READ, MEMBERS_READ);
        };
    }
}
