package com.spexcrafters.platformaccess.api;

import java.util.Set;
import java.util.UUID;

/**
 * The resolved platform-staff authorization context of a moderation request: the staff
 * grant's user, role and capabilities. Produced exclusively by {@link PlatformAccess}.
 */
public record PlatformStaffContext(
        UUID userId,
        PlatformRole role,
        Set<PlatformCapability> capabilities) {

    public boolean has(PlatformCapability capability) {
        return capabilities.contains(capability);
    }
}
