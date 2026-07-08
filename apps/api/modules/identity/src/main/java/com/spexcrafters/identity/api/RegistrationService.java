package com.spexcrafters.identity.api;

import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.identity.domain.EmailVerificationToken;
import com.spexcrafters.identity.domain.OpaqueTokenGenerator;
import com.spexcrafters.identity.domain.PasswordPolicy;
import com.spexcrafters.identity.domain.TokenHasher;
import com.spexcrafters.identity.domain.UserAccount;
import com.spexcrafters.identity.infrastructure.EmailVerificationTokenRepository;
import com.spexcrafters.identity.infrastructure.UserAccountRepository;
import com.spexcrafters.identity.infrastructure.config.AuthProperties;
import com.spexcrafters.identity.infrastructure.mail.VerificationMailer;
import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import com.spexcrafters.sharedkernel.problem.ConflictException;
import com.spexcrafters.sharedkernel.problem.ProblemFieldError;
import com.spexcrafters.sharedkernel.util.UuidV7;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates accounts in {@code PENDING_VERIFICATION} state (Argon2id password hash), issues
 * the single-use verification token and dispatches the verification email.
 */
@Service
public class RegistrationService {

    private final UserAccountRepository users;
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordEncoder passwordEncoder;
    private final VerificationMailer verificationMailer;
    private final AuditLogger auditLogger;
    private final AuthProperties authProperties;
    private final Clock clock;

    public RegistrationService(UserAccountRepository users,
            EmailVerificationTokenRepository verificationTokens,
            PasswordEncoder passwordEncoder,
            VerificationMailer verificationMailer,
            AuditLogger auditLogger,
            AuthProperties authProperties,
            Clock clock) {
        this.users = users;
        this.verificationTokens = verificationTokens;
        this.passwordEncoder = passwordEncoder;
        this.verificationMailer = verificationMailer;
        this.auditLogger = auditLogger;
        this.authProperties = authProperties;
        this.clock = clock;
    }

    @Transactional
    public UUID register(RegisterUserCommand command) {
        String email = normalizeEmail(command.email());

        List<ProblemFieldError> policyViolations = PasswordPolicy.validate(command.password(), email);
        if (!policyViolations.isEmpty()) {
            throw ApiProblemException.validation(policyViolations);
        }
        if (users.existsByEmail(email)) {
            // Backed by the unique citext constraint; a concurrent race surfaces as a
            // DataIntegrityViolationException which the global handler also maps to 409.
            throw new ConflictException("An account with this email address already exists.");
        }

        UUID userId = UuidV7.generate();
        UserAccount user = new UserAccount(
                userId,
                email,
                passwordEncoder.encode(command.password()),
                command.displayName().trim(),
                command.localeCode());
        users.save(user);

        issueVerificationToken(user);
        auditLogger.record("identity.user.registered", userId, "user_account", userId.toString());
        return userId;
    }

    /**
     * Uniform behaviour regardless of account existence or state (no account enumeration):
     * an email is only actually sent when a matching unverified account exists.
     */
    @Transactional
    public void resendVerification(String rawEmail) {
        users.findByEmail(normalizeEmail(rawEmail))
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::issueVerificationToken);
    }

    private void issueVerificationToken(UserAccount user) {
        String rawToken = OpaqueTokenGenerator.generate();
        EmailVerificationToken token = new EmailVerificationToken(
                UuidV7.generate(),
                user.getId(),
                TokenHasher.sha256Hex(rawToken),
                clock.instant().plus(authProperties.verificationTokenTtl()));
        verificationTokens.save(token);
        verificationMailer.sendVerificationEmail(user.getEmail(), user.getDisplayName(), rawToken);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
