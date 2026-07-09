package com.spexcrafters.verification.web;

import com.spexcrafters.sharedkernel.problem.AuthenticationFailedException;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** Resolves the authenticated user id from the JWT {@code sub} claim. */
final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    static UUID id(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new AuthenticationFailedException("Invalid access token subject.");
        }
    }
}
