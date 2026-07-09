package com.spexcrafters.platformaccess.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.spexcrafters.platformaccess.domain.PlatformAuthorizationDeniedException;
import com.spexcrafters.platformaccess.domain.PlatformStaff;
import com.spexcrafters.platformaccess.infrastructure.PlatformStaffRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlatformAccessTest {

    private final PlatformStaffRepository staff = mock(PlatformStaffRepository.class);
    private final PlatformAccessDenialAuditor auditor = mock(PlatformAccessDenialAuditor.class);
    private final PlatformAccess access = new PlatformAccess(staff, auditor);

    @Test
    void nonStaffCallerIsDeniedAndAudited() {
        UUID user = UUID.randomUUID();
        when(staff.findByUserId(user)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> access.require(user, PlatformCapability.REVIEW_READ))
                .isInstanceOf(PlatformAuthorizationDeniedException.class);
        verify(auditor).recordDenied(eq(user), eq("supplier.review.read"));
    }

    @Test
    void reviewerLackingApproveIsDenied() {
        UUID user = UUID.randomUUID();
        when(staff.findByUserId(user))
                .thenReturn(Optional.of(new PlatformStaff(UUID.randomUUID(), user, PlatformRole.REVIEWER)));
        assertThatThrownBy(() -> access.require(user, PlatformCapability.REVIEW_APPROVE))
                .isInstanceOf(PlatformAuthorizationDeniedException.class);
        verify(auditor).recordDenied(eq(user), eq("supplier.review.approve"));
    }

    @Test
    void deactivatedStaffHasNoCapabilities() {
        UUID user = UUID.randomUUID();
        PlatformStaff inactive = mock(PlatformStaff.class);
        when(inactive.isActive()).thenReturn(false);
        when(inactive.capabilities()).thenReturn(java.util.Set.of());
        when(staff.findByUserId(user)).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> access.require(user, PlatformCapability.REVIEW_READ))
                .isInstanceOf(PlatformAuthorizationDeniedException.class);
    }

    @Test
    void seniorReviewerWithCapabilityResolvesContext() {
        UUID user = UUID.randomUUID();
        when(staff.findByUserId(user))
                .thenReturn(Optional.of(new PlatformStaff(UUID.randomUUID(), user, PlatformRole.SENIOR_REVIEWER)));
        PlatformStaffContext context = access.require(user, PlatformCapability.VERIFICATION_GRANT);
        assertThat(context.role()).isEqualTo(PlatformRole.SENIOR_REVIEWER);
        assertThat(context.has(PlatformCapability.VERIFICATION_GRANT)).isTrue();
        verify(auditor, never()).recordDenied(any(), any());
    }
}
