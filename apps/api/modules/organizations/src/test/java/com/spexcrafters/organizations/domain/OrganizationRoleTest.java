package com.spexcrafters.organizations.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Rank rules: strict order OWNER > ADMIN > MEMBER. */
class OrganizationRoleTest {

    @Test
    void rankOrderIsOwnerAdminMember() {
        assertThat(OrganizationRole.OWNER.rank()).isGreaterThan(OrganizationRole.ADMIN.rank());
        assertThat(OrganizationRole.ADMIN.rank()).isGreaterThan(OrganizationRole.MEMBER.rank());
    }

    @Test
    void higherThanIsStrict() {
        assertThat(OrganizationRole.OWNER.higherThan(OrganizationRole.ADMIN)).isTrue();
        assertThat(OrganizationRole.OWNER.higherThan(OrganizationRole.MEMBER)).isTrue();
        assertThat(OrganizationRole.ADMIN.higherThan(OrganizationRole.MEMBER)).isTrue();

        assertThat(OrganizationRole.ADMIN.higherThan(OrganizationRole.ADMIN)).isFalse();
        assertThat(OrganizationRole.ADMIN.higherThan(OrganizationRole.OWNER)).isFalse();
        assertThat(OrganizationRole.MEMBER.higherThan(OrganizationRole.MEMBER)).isFalse();
    }

    @Test
    void isAtLeastIsReflexive() {
        for (OrganizationRole role : OrganizationRole.values()) {
            assertThat(role.isAtLeast(role)).isTrue();
        }
        assertThat(OrganizationRole.MEMBER.isAtLeast(OrganizationRole.ADMIN)).isFalse();
        assertThat(OrganizationRole.ADMIN.isAtLeast(OrganizationRole.OWNER)).isFalse();
        assertThat(OrganizationRole.OWNER.isAtLeast(OrganizationRole.MEMBER)).isTrue();
    }
}
