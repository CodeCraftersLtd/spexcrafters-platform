package com.spexcrafters.platformaccess.domain;

import com.spexcrafters.platformaccess.api.PlatformCapability;
import com.spexcrafters.platformaccess.api.PlatformRole;
import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Set;
import java.util.UUID;

/**
 * A platform-staff grant: it binds an identity user to a {@link PlatformRole}. Provisioned
 * by a documented bootstrap mechanism (Flyway seed / operator insert), never by an email
 * allowlist or a hidden route. Deactivation ({@code active = false}) revokes all platform
 * capabilities without deleting the audit trail of the row.
 */
@Entity
@Table(name = "platform_staff", schema = "platform_access")
public class PlatformStaff extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_role", nullable = false, length = 32)
    private PlatformRole platformRole;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected PlatformStaff() {
        // JPA
    }

    public PlatformStaff(UUID id, UUID userId, PlatformRole platformRole) {
        if (id == null || userId == null || platformRole == null) {
            throw new IllegalArgumentException("id, userId and platformRole are required");
        }
        this.id = id;
        this.userId = userId;
        this.platformRole = platformRole;
        this.active = true;
    }

    /** The capabilities this grant currently confers (empty when deactivated). */
    public Set<PlatformCapability> capabilities() {
        return active ? PlatformCapability.forRole(platformRole) : Set.of();
    }

    public boolean isActive() {
        return active;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public PlatformRole getPlatformRole() {
        return platformRole;
    }
}
