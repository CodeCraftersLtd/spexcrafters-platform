package com.spexcrafters.organizations.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pins the role→capability matrix of organizations-capability-model.md §2 exactly. */
class CapabilityTest {

    @Test
    void ownerHoldsEveryCapability() {
        assertThat(Capability.forRole(OrganizationRole.OWNER))
                .containsExactlyInAnyOrder(Capability.values());
    }

    @Test
    void adminHoldsEverythingExceptRolesManage() {
        assertThat(Capability.forRole(OrganizationRole.ADMIN)).containsExactlyInAnyOrder(
                Capability.ORGANIZATION_READ,
                Capability.ORGANIZATION_UPDATE,
                Capability.MEMBERS_READ,
                Capability.MEMBERS_INVITE,
                Capability.MEMBERS_REMOVE);
    }

    @Test
    void memberHoldsReadCapabilitiesOnly() {
        assertThat(Capability.forRole(OrganizationRole.MEMBER)).containsExactlyInAnyOrder(
                Capability.ORGANIZATION_READ,
                Capability.MEMBERS_READ);
    }

    @Test
    void forRoleReturnsImmutableSets() {
        Set<Capability> capabilities = Capability.forRole(OrganizationRole.MEMBER);
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> capabilities.add(Capability.ROLES_MANAGE));
    }

    @Test
    void wireNamesMatchTheOpenApiContract() {
        assertThat(Capability.ORGANIZATION_READ.wireName()).isEqualTo("organization.read");
        assertThat(Capability.ORGANIZATION_UPDATE.wireName()).isEqualTo("organization.update");
        assertThat(Capability.MEMBERS_READ.wireName()).isEqualTo("organization.members.read");
        assertThat(Capability.MEMBERS_INVITE.wireName()).isEqualTo("organization.members.invite");
        assertThat(Capability.MEMBERS_REMOVE.wireName()).isEqualTo("organization.members.remove");
        assertThat(Capability.ROLES_MANAGE.wireName()).isEqualTo("organization.roles.manage");
    }
}
