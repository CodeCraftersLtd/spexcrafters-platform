package com.spexcrafters.organizations.web;

import com.spexcrafters.organizations.api.MemberDto;
import com.spexcrafters.organizations.api.MembershipService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Member sub-resource of the {@code organizations} tag. */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/members")
public class MemberController {

    private final MembershipService membershipService;

    public MemberController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    /** operationId: listMembers */
    @GetMapping
    public List<MemberDto> listMembers(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId) {
        return membershipService.listMembers(AuthenticatedUser.id(jwt), organizationId);
    }

    /** operationId: removeMember */
    @DeleteMapping("/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId,
            @PathVariable UUID membershipId) {
        membershipService.removeMember(AuthenticatedUser.id(jwt), organizationId, membershipId);
    }

    /** operationId: changeMemberRole */
    @PutMapping("/{membershipId}/role")
    public MemberDto changeMemberRole(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId,
            @PathVariable UUID membershipId,
            @Valid @RequestBody ChangeRoleRequest request) {
        return membershipService.changeRole(
                AuthenticatedUser.id(jwt), organizationId, membershipId, request.role());
    }
}
