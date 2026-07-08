package com.spexcrafters.organizations.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A user's membership in an organization. {@code REMOVED} is terminal: removed rows are
 * retained for audit, and re-joining creates a new row (enforced by the partial unique
 * index on {@code (organization_id, user_id) WHERE status = 'ACTIVE'}).
 */
@Entity
@Table(name = "membership", schema = "organizations")
public class OrganizationMembership extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private OrganizationRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MembershipStatus status;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    protected OrganizationMembership() {
        // JPA
    }

    public OrganizationMembership(UUID id, UUID organizationId, UUID userId,
            OrganizationRole role, Instant joinedAt) {
        if (id == null || organizationId == null || userId == null) {
            throw new IllegalArgumentException("id, organizationId and userId are required");
        }
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
        if (joinedAt == null) {
            throw new IllegalArgumentException("joinedAt is required");
        }
        this.id = id;
        this.organizationId = organizationId;
        this.userId = userId;
        this.role = role;
        this.status = MembershipStatus.ACTIVE;
        this.joinedAt = joinedAt;
    }

    /** Terminal transition {@code ACTIVE → REMOVED}; the row is kept for audit. */
    public void remove(Instant now) {
        if (status != MembershipStatus.ACTIVE) {
            throw new IllegalStateException("Only an ACTIVE membership can be removed");
        }
        this.status = MembershipStatus.REMOVED;
        this.removedAt = now;
    }

    public void changeRole(OrganizationRole newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("role is required");
        }
        if (status != MembershipStatus.ACTIVE) {
            throw new IllegalStateException("Only an ACTIVE membership can change role");
        }
        this.role = newRole;
    }

    public boolean isActive() {
        return status == MembershipStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public OrganizationRole getRole() {
        return role;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Instant getRemovedAt() {
        return removedAt;
    }
}
