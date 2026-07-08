package com.spexcrafters.organizations.web;

import com.spexcrafters.organizations.api.InvitationDto;
import com.spexcrafters.organizations.api.InvitationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Invitation sub-resource of the {@code organizations} tag. */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    /** operationId: listInvitations */
    @GetMapping
    public List<InvitationDto> listInvitations(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId) {
        return invitationService.list(AuthenticatedUser.id(jwt), organizationId);
    }

    /** operationId: createInvitation */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationDto createInvitation(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateInvitationRequest request) {
        return invitationService.create(
                AuthenticatedUser.id(jwt), organizationId, request.email(), request.role());
    }

    /** operationId: revokeInvitation */
    @PostMapping("/{invitationId}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeInvitation(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId,
            @PathVariable UUID invitationId) {
        invitationService.revoke(AuthenticatedUser.id(jwt), organizationId, invitationId);
    }
}
