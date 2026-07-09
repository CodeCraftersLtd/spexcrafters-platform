package com.spexcrafters.platformaccess.api;

import com.spexcrafters.platformaccess.domain.PlatformAuthorizationDeniedException;
import com.spexcrafters.platformaccess.domain.PlatformStaff;
import com.spexcrafters.platformaccess.infrastructure.PlatformStaffRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The authorization policy of the platform-access context (supplier-domain-model §6).
 * Platform moderation capabilities are resolved from an active {@link PlatformStaff} grant —
 * never from an organization role, so an org OWNER can never obtain them. Every denial emits
 * an {@code authorization.denied} audit event that survives the caller's rollback.
 */
@Service
public class PlatformAccess {

    private final PlatformStaffRepository staff;
    private final PlatformAccessDenialAuditor denialAuditor;

    public PlatformAccess(PlatformStaffRepository staff, PlatformAccessDenialAuditor denialAuditor) {
        this.staff = staff;
        this.denialAuditor = denialAuditor;
    }

    /**
     * Requires an active platform-staff grant that confers {@code capability}. A non-staff or
     * deactivated caller, or one whose role lacks the capability, receives an audited 403.
     */
    @Transactional(readOnly = true)
    public PlatformStaffContext require(UUID userId, PlatformCapability capability) {
        Optional<PlatformStaff> grant = userId == null ? Optional.empty() : staff.findByUserId(userId);
        if (grant.isEmpty() || !grant.get().isActive() || !grant.get().capabilities().contains(capability)) {
            denialAuditor.recordDenied(userId, capability.wireName());
            throw new PlatformAuthorizationDeniedException();
        }
        PlatformStaff resolved = grant.get();
        return new PlatformStaffContext(resolved.getUserId(), resolved.getPlatformRole(), resolved.capabilities());
    }

    /** True when the caller is active platform staff (used to widen read visibility). */
    @Transactional(readOnly = true)
    public boolean isActiveStaff(UUID userId) {
        return userId != null && staff.findByUserId(userId).map(PlatformStaff::isActive).orElse(false);
    }
}
