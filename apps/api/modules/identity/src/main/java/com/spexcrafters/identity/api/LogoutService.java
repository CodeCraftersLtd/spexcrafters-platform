package com.spexcrafters.identity.api;

import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.identity.domain.TokenHasher;
import com.spexcrafters.identity.infrastructure.RefreshTokenRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revokes the whole refresh-token family the presented token belongs to. Idempotent:
 * unknown or already-revoked tokens are a silent no-op (the endpoint always returns 204).
 */
@Service
public class LogoutService {

    private final RefreshTokenRepository refreshTokens;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public LogoutService(RefreshTokenRepository refreshTokens, AuditLogger auditLogger, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.findByTokenHash(TokenHasher.sha256Hex(rawRefreshToken)).ifPresent(token -> {
            int revoked = refreshTokens.revokeFamily(token.getFamilyId(), clock.instant());
            if (revoked > 0) {
                auditLogger.record("identity.user.logout", token.getUserId(),
                        "refresh_token_family", token.getFamilyId().toString());
            }
        });
    }
}
