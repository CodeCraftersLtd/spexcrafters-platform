package com.spexcrafters.organizations.web;

import com.spexcrafters.organizations.api.MyMembershipDto;
import com.spexcrafters.organizations.api.MyOrganizationsService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The caller's own memberships (part of the {@code organizations} tag). */
@RestController
public class MyOrganizationsController {

    private final MyOrganizationsService myOrganizationsService;

    public MyOrganizationsController(MyOrganizationsService myOrganizationsService) {
        this.myOrganizationsService = myOrganizationsService;
    }

    /** operationId: listMyOrganizations */
    @GetMapping("/api/v1/me/organizations")
    public List<MyMembershipDto> listMyOrganizations(@AuthenticationPrincipal Jwt jwt) {
        return myOrganizationsService.list(AuthenticatedUser.id(jwt));
    }
}
