package com.spexcrafters.organizations.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.spexcrafters.sharedkernel.util.UuidV7;
import org.junit.jupiter.api.Test;

class OrganizationTest {

    @Test
    void newOrganizationIsActiveAndTrimsItsName() {
        Organization organization = new Organization(
                UuidV7.generate(), "  Acme Optics  ", OrganizationType.SUPPLIER, "DE");

        assertThat(organization.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
        assertThat(organization.isActive()).isTrue();
        assertThat(organization.getName()).isEqualTo("Acme Optics");
        assertThat(organization.getCountry()).isEqualTo("DE");
    }

    @Test
    void countryIsOptional() {
        Organization organization = new Organization(
                UuidV7.generate(), "Acme Optics", OrganizationType.BUYER, null);
        assertThat(organization.getCountry()).isNull();

        organization.assignCountry("SG");
        assertThat(organization.getCountry()).isEqualTo("SG");

        organization.assignCountry(null);
        assertThat(organization.getCountry()).isNull();
    }

    @Test
    void rejectsInvalidNames() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new Organization(UuidV7.generate(), null, OrganizationType.BUYER, null));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new Organization(UuidV7.generate(), " ", OrganizationType.BUYER, null));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new Organization(UuidV7.generate(), "A", OrganizationType.BUYER, null));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new Organization(UuidV7.generate(), "x".repeat(121), OrganizationType.BUYER, null));
    }

    @Test
    void rejectsInvalidCountryCodes() {
        Organization organization = new Organization(
                UuidV7.generate(), "Acme Optics", OrganizationType.HYBRID, null);

        assertThatIllegalArgumentException().isThrownBy(() -> organization.assignCountry("de"));
        assertThatIllegalArgumentException().isThrownBy(() -> organization.assignCountry("DEU"));
        assertThatIllegalArgumentException().isThrownBy(() -> organization.assignCountry("D1"));
    }

    @Test
    void renameValidatesLikeTheConstructor() {
        Organization organization = new Organization(
                UuidV7.generate(), "Acme Optics", OrganizationType.HYBRID, null);

        organization.rename("  New Name  ");
        assertThat(organization.getName()).isEqualTo("New Name");

        assertThatIllegalArgumentException().isThrownBy(() -> organization.rename(""));
    }
}
