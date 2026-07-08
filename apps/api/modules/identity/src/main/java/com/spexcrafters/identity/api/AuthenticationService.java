package com.spexcrafters.identity.api;

import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.identity.domain.EmailNotVerifiedException;
import com.spexcrafters.identity.domain.LoginAttempt;
import com.spexcrafters.identity.domain.LoginOutcome;
import com.spexcrafters.identity.domain.LoginThrottle;
import com.spexcrafters.identity.domain.OpaqueTokenGenerator;
import com.spexcrafters.identity.domain.RefreshToken;
import com.spexcrafters.identity.domain.TokenHasher;
import com.spexcrafters.identity.domain.UserAccount;
import com.spexcrafters.identity.domain.UserStatus;
import com.spexcrafters.identity.infrastructure.LoginAttemptRepository;
import com.spexcrafters.identity.infrastructure.RefreshTokenRepository;
import com.spexcrafters.identity.infrastructure.UserAccountRepository;
import com.spexcrafters.identity.infrastructure.config.AuthProperties;
import com.spexcrafters.identity.infrastructure.security.JwtIssuer;
import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import com.spexcrafters.sharedkernel.problem.AuthenticationFailedException;
import com.spexcrafters.sharedkernel.problem.RateLimitedException;
import com.spexcrafters.sharedkernel.util.UuidV7;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Password login, refresh-token rotation and theft detection.
 *
 * <p>Both public methods use {@code noRollbackFor = ApiProblemException} deliberately:
 * a 401/429 response must still commit its side effects — the recorded login attempt
 * (otherwise brute-force counting would never accumulate) and the family revocation
 * performed when refresh-token reuse is detected.
 */
@Service
public class AuthenticationService {

    private static final String BAD_CREDENTIALS = "Invalid email or password.";
    private static final String INVALID_REFRESH_TOKEN = "Refresh token is invalid, expired or revoked.";

    private final UserAccountRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final LoginAttemptRepository loginAttempts;
    private final PasswordEncoder passwordEncoder;
    private final JwtIssuer jwtIssuer;
    private final AuditLogger auditLogger;
    private final AuthProperties authProperties;
    private final Clock clock;

    /**
     * A real Argon2id hash of a random throwaway value, matched against when the email is
     * unknown so unknown-vs-wrong-password cannot be distinguished by response timing.
     */
    private final String timingEqualizerHash;

    public AuthenticationService(UserAccountRepository users,
            RefreshTokenRepository refreshTokens,
            LoginAttemptRepository loginAttempts,
            PasswordEncoder passwordEncoder,
            JwtIssuer jwtIssuer,
            AuditLogger auditLogger,
            AuthProperties authProperties,
            Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.loginAttempts = loginAttempts;
        this.passwordEncoder = passwordEncoder;
        this.jwtIssuer = jwtIssuer;
        this.auditLogger = auditLogger;
        this.authProperties = authProperties;
        this.clock = clock;
        this.timingEqualizerHash = passwordEncoder.encode(OpaqueTokenGenerator.generate());
    }

    @Transactional(noRollbackFor = ApiProblemException.class)
    public IssuedTokens login(LoginCommand command) {
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        Instant now = clock.instant();

        throttleOrThrow(email, command.ipAddress(), now);

        Optional<UserAccount> maybeUser = users.findByEmail(email);
        boolean passwordMatches = maybeUser
                .map(user -> passwordEncoder.matches(command.password(), user.getPasswordHash()))
                .orElseGet(() -> {
                    passwordEncoder.matches(command.password(), timingEqualizerHash);
                    return false;
                });
        if (!passwordMatches) {
            recordAttempt(email, maybeUser.map(UserAccount::getId).orElse(null),
                    LoginOutcome.FAILURE, command.ipAddress(), now);
            throw new AuthenticationFailedException(BAD_CREDENTIALS);
        }

        UserAccount user = maybeUser.orElseThrow();
        if (user.getStatus() == UserStatus.SUSPENDED) {
            recordAttempt(email, user.getId(), LoginOutcome.SUSPENDED, command.ipAddress(), now);
            // Deliberately indistinguishable from bad credentials: no account-state oracle.
            throw new AuthenticationFailedException(BAD_CREDENTIALS);
        }
        if (!user.isEmailVerified()) {
            recordAttempt(email, user.getId(), LoginOutcome.EMAIL_NOT_VERIFIED, command.ipAddress(), now);
            throw new EmailNotVerifiedException();
        }

        user.recordSuccessfulLogin(now);
        recordAttempt(email, user.getId(), LoginOutcome.SUCCESS, command.ipAddress(), now);
        auditLogger.record("identity.user.login", user.getId(), "user_account", user.getId().toString());

        // A fresh login starts a fresh token family.
        MintedRefreshToken minted = mintRefreshToken(user, UUID.randomUUID(), now);
        return buildTokens(user, minted, now);
    }

    /**
     * Rotates a refresh token: the presented token is consumed and a successor in the same
     * family is issued. Presenting an already-consumed or revoked token revokes the whole
     * family (theft detection) — and that revocation commits despite the thrown 401.
     */
    @Transactional(noRollbackFor = ApiProblemException.class)
    public IssuedTokens refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokens.findByTokenHash(TokenHasher.sha256Hex(rawRefreshToken))
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_REFRESH_TOKEN));

        if (current.isConsumedOrRevoked()) {
            refreshTokens.revokeFamily(current.getFamilyId(), now);
            auditLogger.record("identity.refresh_token.reuse_detected", current.getUserId(),
                    "refresh_token_family", current.getFamilyId().toString());
            throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN);
        }
        if (current.isExpired(now)) {
            throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN);
        }

        UserAccount user = users.findById(current.getUserId())
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_REFRESH_TOKEN));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN);
        }

        MintedRefreshToken minted = mintRefreshToken(user, current.getFamilyId(), now);
        current.markReplacedBy(minted.entity().getId());
        return buildTokens(user, minted, now);
    }

    private void throttleOrThrow(String email, String ipAddress, Instant now) {
        Instant windowStart = now.minus(LoginThrottle.WINDOW);
        long failures = loginAttempts.countByEmailAndOutcomeAndAtAfter(email, LoginOutcome.FAILURE, windowStart);
        Instant earliestFailure = loginAttempts
                .findFirstByEmailAndOutcomeAndAtAfterOrderByAtAsc(email, LoginOutcome.FAILURE, windowStart)
                .map(LoginAttempt::getAt)
                .orElse(now);
        OptionalLong retryAfter = LoginThrottle.retryAfterSeconds(failures, earliestFailure, now);
        if (retryAfter.isPresent()) {
            recordAttempt(email, null, LoginOutcome.THROTTLED, ipAddress, now);
            throw new RateLimitedException("Too many failed login attempts. Try again later.",
                    retryAfter.getAsLong());
        }
    }

    private MintedRefreshToken mintRefreshToken(UserAccount user, UUID familyId, Instant now) {
        String raw = OpaqueTokenGenerator.generate();
        RefreshToken entity = new RefreshToken(
                UuidV7.generate(),
                user.getId(),
                TokenHasher.sha256Hex(raw),
                familyId,
                now.plus(authProperties.refreshTokenTtl()));
        refreshTokens.save(entity);
        return new MintedRefreshToken(raw, entity);
    }

    private IssuedTokens buildTokens(UserAccount user, MintedRefreshToken minted, Instant now) {
        return new IssuedTokens(
                jwtIssuer.issueAccessToken(user.getId(), user.getEmail(), now),
                authProperties.accessTokenTtl().toSeconds(),
                minted.raw(),
                UserSummaries.toDto(user));
    }

    private void recordAttempt(String email, UUID userId, LoginOutcome outcome, String ipAddress, Instant at) {
        String ip = ipAddress != null && ipAddress.length() > 45 ? ipAddress.substring(0, 45) : ipAddress;
        loginAttempts.save(new LoginAttempt(UuidV7.generate(), userId, email, ip, outcome, at));
    }

    private record MintedRefreshToken(String raw, RefreshToken entity) {
    }
}
