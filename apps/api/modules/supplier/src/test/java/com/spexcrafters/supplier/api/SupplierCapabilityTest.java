package com.spexcrafters.supplier.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.spexcrafters.organizations.api.OrgRole;
import org.junit.jupiter.api.Test;

class SupplierCapabilityTest {

    @Test
    void ownerAndAdminHoldEveryCapability() {
        assertThat(SupplierCapability.forRole(OrgRole.OWNER))
                .containsExactlyInAnyOrder(SupplierCapability.values());
        assertThat(SupplierCapability.forRole(OrgRole.ADMIN))
                .containsExactlyInAnyOrder(SupplierCapability.values());
    }

    @Test
    void memberIsReadOnly() {
        assertThat(SupplierCapability.forRole(OrgRole.MEMBER))
                .containsExactlyInAnyOrder(SupplierCapability.READ, SupplierCapability.VERIFICATION_READ);
    }

    @Test
    void memberCannotCreateUpdateSubmitOrUploadEvidence() {
        var member = SupplierCapability.forRole(OrgRole.MEMBER);
        assertThat(member).doesNotContain(SupplierCapability.CREATE, SupplierCapability.UPDATE,
                SupplierCapability.SUBMIT, SupplierCapability.WITHDRAW,
                SupplierCapability.EVIDENCE_UPLOAD, SupplierCapability.EVIDENCE_DELETE);
    }

    @Test
    void wireNamesAreStableDottedCodes() {
        assertThat(SupplierCapability.EVIDENCE_UPLOAD.wireName()).isEqualTo("supplier.evidence.upload");
        assertThat(SupplierCapability.VERIFICATION_READ.wireName()).isEqualTo("supplier.verification.read");
    }
}
