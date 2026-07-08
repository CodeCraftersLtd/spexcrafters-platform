package com.spexcrafters.identity.api;

import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.identity.domain.EmailVerificationToken;
import com.spexcrafters.identity.domain.TokenHasher;
import com.spexcrafters.identity.domain.UserAccount;
import com.spexcrafters.identity.infrastructure.EmailVerificationTokenRepository;
import com.spexcrafters.identity.infrastructure.UserAccountRepository;
import com.spexcrafters.sharedkernel.problem.ResourceGoneException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirms email addresses with a single-use, expiring token. Idempotent for a consumed
 * token whose owner is already verified (the contract's 204 idempotency clause); every
 * other invalid case is a uniform 410 so token state cannot be probed.
 */
@Service
public class EmailVerificationService {

    private static final String GONE_DETAIL =
            "This verification link is invalid, expired or has already been used. Request a new one.";

    private final EmailVerificationTokenRepository verificationTokens;
    private final UserAccountRepository users;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public EmailVerificationService(EmailVerificationTokenRepository verificationTokens,
            UserAccountRepository users,
            AuditLogger auditLogger,
            Clock clock) {
        this.verificationTokens = verificationTokens;
        this.users = users;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Transactional
    public void verify(String rawToken) {
        EmailVerificationToken token = verificationTokens.findByTokenHash(TokenHasher.sha256Hex(rawToken))
                .orElseThrow(() -> new ResourceGoneException(GONE_DETAIL));
        UserAccount user = users.findById(token.getUserId())
                .orElseThrow(() -> new ResourceGoneException(GONE_DETAIL));

        if (token.isUsed()) {
            if (user.isEmailVerified()) {
                return; // idempotent re-submit of an already-consumed link
            }
            throw new ResourceGoneException(GONE_DETAIL);
        }

        Instant now = clock.instant();
        if (token.isExpired(now)) {
            throw new ResourceGoneException(GONE_DETAIL);
        }

        token.markUsed(now);
        user.markEmailVerified(now);
        auditLogger.record("identity.email.verified", user.getId(), "user_account", user.getId().toString());
    }
}
