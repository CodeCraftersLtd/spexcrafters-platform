package com.spexcrafters.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * Append-only record of a login attempt; queried for in-database brute-force detection.
 * Implements {@link Persistable} so pre-assigned UUIDv7 ids persist without a merge-select.
 */
@Entity
@Table(name = "login_attempt", schema = "identity")
public class LoginAttempt implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email", nullable = false, columnDefinition = "citext")
    private String email;

    @Column(name = "ip", length = 45)
    private String ip;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 32)
    private LoginOutcome outcome;

    @Column(name = "at", nullable = false)
    private Instant at;

    @Transient
    private boolean isNew = true;

    protected LoginAttempt() {
        // JPA
    }

    public LoginAttempt(UUID id, UUID userId, String email, String ip, LoginOutcome outcome, Instant at) {
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.ip = ip;
        this.outcome = outcome;
        this.at = at;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getIp() {
        return ip;
    }

    public LoginOutcome getOutcome() {
        return outcome;
    }

    public Instant getAt() {
        return at;
    }
}
