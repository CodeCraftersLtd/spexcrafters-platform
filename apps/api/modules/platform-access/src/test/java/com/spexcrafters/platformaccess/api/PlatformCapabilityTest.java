package com.spexcrafters.platformaccess.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlatformCapabilityTest {

    @Test
    void reviewerCanReadClaimAndRequestChangesOnly() {
        assertThat(PlatformCapability.forRole(PlatformRole.REVIEWER))
                .containsExactlyInAnyOrder(PlatformCapability.REVIEW_READ, PlatformCapability.REVIEW_CLAIM,
                        PlatformCapability.REVIEW_REQUEST_CHANGES);
    }

    @Test
    void reviewerCannotApproveRejectOrTouchVerification() {
        var reviewer = PlatformCapability.forRole(PlatformRole.REVIEWER);
        assertThat(reviewer).doesNotContain(PlatformCapability.REVIEW_APPROVE, PlatformCapability.REVIEW_REJECT,
                PlatformCapability.VERIFICATION_GRANT, PlatformCapability.SUPPLIER_SUSPEND);
    }

    @Test
    void seniorReviewerCanDecideAndManageVerificationButNotSuspendSupplier() {
        var senior = PlatformCapability.forRole(PlatformRole.SENIOR_REVIEWER);
        assertThat(senior).contains(PlatformCapability.REVIEW_APPROVE, PlatformCapability.REVIEW_REJECT,
                PlatformCapability.VERIFICATION_GRANT, PlatformCapability.VERIFICATION_SUSPEND,
                PlatformCapability.VERIFICATION_REVOKE);
        assertThat(senior).doesNotContain(PlatformCapability.SUPPLIER_SUSPEND);
    }

    @Test
    void platformAdminHoldsEveryCapability() {
        assertThat(PlatformCapability.forRole(PlatformRole.PLATFORM_ADMIN))
                .containsExactlyInAnyOrder(PlatformCapability.values());
    }
}
