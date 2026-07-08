package com.spexcrafters.identity.domain;

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
 * A platform user. Email is stored in a PostgreSQL {@code citext} column, making
 * lookups and the unique constraint case-insensitive at the database level; the
 * application additionally normalises emails to lower case before persisting.
 */
@Entity
@Table(name = "user_account", schema = "identity")
public class UserAccount extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "email", nullable = false, columnDefinition = "citext")
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "locale", nullable = false, length = 16)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatus status;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected UserAccount() {
        // JPA
    }

    public UserAccount(UUID id, String email, String passwordHash, String displayName, String locale) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.locale = locale;
        this.status = UserStatus.PENDING_VERIFICATION;
    }

    /** Idempotent: verifying an already-verified account is a no-op. */
    public void markEmailVerified(Instant now) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = now;
            if (status == UserStatus.PENDING_VERIFICATION) {
                status = UserStatus.ACTIVE;
            }
        }
    }

    public void recordSuccessfulLogin(Instant now) {
        this.lastLoginAt = now;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLocale() {
        return locale;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
}
