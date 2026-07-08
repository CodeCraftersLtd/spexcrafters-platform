package com.spexcrafters.organizations.web;

import com.spexcrafters.organizations.api.InvitationService;
import com.spexcrafters.organizations.api.MyMembershipDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Top-level invitation acceptance endpoint (the token, not the URL, scopes the org). */
@RestController
public class InvitationAcceptController {

    private final InvitationService invitationService;

    public InvitationAcceptController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    /** operationId: acceptInvitation */
    @PostMapping("/api/v1/invitations/accept")
    public MyMembershipDto acceptInvitation(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AcceptInvitationRequest request) {
        return invitationService.accept(AuthenticatedUser.id(jwt), request.token());
    }
}
