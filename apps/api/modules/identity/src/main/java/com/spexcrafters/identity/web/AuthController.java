package com.spexcrafters.identity.web;

import com.spexcrafters.identity.api.AuthenticationService;
import com.spexcrafters.identity.api.EmailVerificationService;
import com.spexcrafters.identity.api.LoginCommand;
import com.spexcrafters.identity.api.LogoutService;
import com.spexcrafters.identity.api.RegisterUserCommand;
import com.spexcrafters.identity.api.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The {@code auth} tag of the OpenAPI contract. Thin by design: validation is declarative,
 * business logic lives in the identity application services, and error mapping is owned by
 * the shared-kernel problem handler.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final EmailVerificationService emailVerificationService;
    private final AuthenticationService authenticationService;
    private final LogoutService logoutService;

    public AuthController(RegistrationService registrationService,
            EmailVerificationService emailVerificationService,
            AuthenticationService authenticationService,
            LogoutService logoutService) {
        this.registrationService = registrationService;
        this.emailVerificationService = emailVerificationService;
        this.authenticationService = authenticationService;
        this.logoutService = logoutService;
    }

    /** operationId: register */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        String localeCode = request.locale() != null ? request.locale().code() : UserLocale.EN.code();
        UUID userId = registrationService.register(new RegisterUserCommand(
                request.email(), request.password(), request.displayName(), localeCode));
        return new RegisterResponse(userId);
    }

    /** operationId: verifyEmail */
    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verify(request.token());
    }

    /** operationId: resendVerification — uniform 202, no account enumeration. */
    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        registrationService.resendVerification(request.email());
    }

    /** operationId: login */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return TokenResponse.from(authenticationService.login(
                new LoginCommand(request.email(), request.password(), clientIp(httpRequest))));
    }

    /** operationId: refreshTokens */
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return TokenResponse.from(authenticationService.refresh(request.refreshToken()));
    }

    /** operationId: logout — idempotent 204. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        logoutService.logout(request.refreshToken());
    }

    /**
     * Client IP for login forensics: first hop of {@code X-Forwarded-For} when running
     * behind the edge proxy, otherwise the socket address.
     */
    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
