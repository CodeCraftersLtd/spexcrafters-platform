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
 * An email invitation into an organization. Only the SHA-256 hash of the single-use token
 * is stored; the raw token appears exactly once in the invitation email and is never
 * logged. OWNER cannot be invited (promote after joining) — enforced here and by the
 * database CHECK constraint.
 */
@Entity
@Table(name = "invitation", schema = "organizations")
public class OrganizationInvitation extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "email", nullable = false, columnDefinition = "citext")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private OrganizationRole role;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InvitationStatus status;

    @Column(name = "invited_by", nullable = false, updatable = false)
    private UUID invitedBy;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "accepted_by")
    private UUID acceptedBy;

    protected OrganizationInvitation() {
        // JPA
    }

    public OrganizationInvitation(UUID id, UUID organizationId, String email, OrganizationRole role,
            String tokenHash, UUID invitedBy, Instant expiresAt) {
        if (id == null || organizationId == null || invitedBy == null) {
            throw new IllegalArgumentException("id, organizationId and invitedBy are required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        if (role == null || role == OrganizationRole.OWNER) {
            throw new IllegalArgumentException("Invitation role must be ADMIN or MEMBER");
        }
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("tokenHash is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt is required");
        }
        this.id = id;
        this.organizationId = organizationId;
        this.email = email;
        this.role = role;
        this.tokenHash = tokenHash;
        this.status = InvitationStatus.PENDING;
        this.invitedBy = invitedBy;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    /** Single-use consumption {@code PENDING → ACCEPTED}; rejects expired tokens. */
    public void markAccepted(Instant now, UUID acceptedByUserId) {
        if (status != InvitationStatus.PENDING) {
            throw new IllegalStateException("Only a PENDING invitation can be accepted");
        }
        if (isExpired(now)) {
            throw new IllegalStateException("An expired invitation cannot be accepted");
        }
        if (acceptedByUserId == null) {
            throw new IllegalArgumentException("acceptedBy is required");
        }
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedAt = now;
        this.acceptedBy = acceptedByUserId;
    }

    public void markRevoked() {
        if (status != InvitationStatus.PENDING) {
            throw new IllegalStateException("Only a PENDING invitation can be revoked");
        }
        this.status = InvitationStatus.REVOKED;
    }

    /** Lazy transition applied when a past-expiry PENDING token is presented. */
    public void markExpired() {
        if (status != InvitationStatus.PENDING) {
            throw new IllegalStateException("Only a PENDING invitation can expire");
        }
        this.status = InvitationStatus.EXPIRED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getEmail() {
        return email;
    }

    public OrganizationRole getRole() {
        return role;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public UUID getAcceptedBy() {
        return acceptedBy;
    }
}
