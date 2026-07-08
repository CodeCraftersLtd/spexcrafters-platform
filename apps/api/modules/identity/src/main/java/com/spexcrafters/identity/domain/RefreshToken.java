package com.spexcrafters.identity.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Opaque, single-use, rotating refresh token (stored as a SHA-256 hex hash).
 *
 * <p>All tokens descending from one login share a {@code familyId}. A token is consumed by
 * setting {@code replacedBy} to its successor's id; presenting a consumed or revoked token
 * again is treated as theft and revokes the entire family.
 */
@Entity
@Table(name = "refresh_token", schema = "identity")
public class RefreshToken extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    protected RefreshToken() {
        // JPA
    }

    public RefreshToken(UUID id, UUID userId, String tokenHash, UUID familyId, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
    }

    public boolean isConsumedOrRevoked() {
        return replacedBy != null || revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void markReplacedBy(UUID successorId) {
        this.replacedBy = successorId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public UUID getReplacedBy() {
        return replacedBy;
    }
}
