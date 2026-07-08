package com.spexcrafters.organizations.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.spexcrafters.sharedkernel.util.UuidV7;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrganizationMembershipTest {

    private static final Instant JOINED = Instant.parse("2026-07-01T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-07-02T10:00:00Z");

    private OrganizationMembership membership(OrganizationRole role) {
        return new OrganizationMembership(
                UuidV7.generate(), UUID.randomUUID(), UUID.randomUUID(), role, JOINED);
    }

    @Test
    void newMembershipIsActive() {
        OrganizationMembership membership = membership(OrganizationRole.MEMBER);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(membership.isActive()).isTrue();
        assertThat(membership.getJoinedAt()).isEqualTo(JOINED);
        assertThat(membership.getRemovedAt()).isNull();
    }

    @Test
    void removeIsTerminal() {
        OrganizationMembership membership = membership(OrganizationRole.MEMBER);

        membership.remove(LATER);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.REMOVED);
        assertThat(membership.isActive()).isFalse();
        assertThat(membership.getRemovedAt()).isEqualTo(LATER);

        assertThatIllegalStateException().isThrownBy(() -> membership.remove(LATER));
    }

    @Test
    void changeRoleRequiresAnActiveMembership() {
        OrganizationMembership membership = membership(OrganizationRole.MEMBER);

        membership.changeRole(OrganizationRole.OWNER);
        assertThat(membership.getRole()).isEqualTo(OrganizationRole.OWNER);

        membership.remove(LATER);
        assertThatIllegalStateException()
                .isThrownBy(() -> membership.changeRole(OrganizationRole.MEMBER));
    }

    @Test
    void changeRoleRejectsNull() {
        OrganizationMembership membership = membership(OrganizationRole.ADMIN);
        assertThatIllegalArgumentException().isThrownBy(() -> membership.changeRole(null));
    }

    @Test
    void constructorRejectsMissingFields() {
        UUID id = UuidV7.generate();
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        assertThatIllegalArgumentException().isThrownBy(
                () -> new OrganizationMembership(null, orgId, userId, OrganizationRole.MEMBER, JOINED));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new OrganizationMembership(id, null, userId, OrganizationRole.MEMBER, JOINED));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new OrganizationMembership(id, orgId, null, OrganizationRole.MEMBER, JOINED));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new OrganizationMembership(id, orgId, userId, null, JOINED));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new OrganizationMembership(id, orgId, userId, OrganizationRole.MEMBER, null));
    }
}
