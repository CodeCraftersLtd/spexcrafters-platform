package com.spexcrafters.identity.web;

import com.spexcrafters.identity.api.CurrentUserService;
import com.spexcrafters.identity.api.UserSummaryDto;
import com.spexcrafters.sharedkernel.problem.AuthenticationFailedException;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The {@code me} tag of the OpenAPI contract. */
@RestController
public class MeController {

    private final CurrentUserService currentUserService;

    public MeController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    /** operationId: getCurrentUser */
    @GetMapping("/api/v1/me")
    public UserSummaryDto getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new AuthenticationFailedException("Invalid access token subject.");
        }
        return currentUserService.getById(userId);
    }
}
