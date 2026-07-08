package com.spexcrafters.organizations.web;

import com.spexcrafters.organizations.api.OrganizationDto;
import com.spexcrafters.organizations.api.OrganizationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Organization resource of the {@code organizations} tag. Thin by design: validation is
 * declarative, authorization and business logic live in the application services, and
 * error mapping is owned by the shared-kernel problem handler.
 */
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /** operationId: createOrganization */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationDto createOrganization(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrganizationRequest request) {
        return organizationService.create(
                AuthenticatedUser.id(jwt), request.name(), request.type(), request.country());
    }

    /** operationId: getOrganization */
    @GetMapping("/{organizationId}")
    public OrganizationDto getOrganization(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId) {
        return organizationService.get(AuthenticatedUser.id(jwt), organizationId);
    }

    /** operationId: updateOrganization */
    @PatchMapping("/{organizationId}")
    public OrganizationDto updateOrganization(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID organizationId,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        return organizationService.update(AuthenticatedUser.id(jwt), organizationId,
                request.name(), request.country(), request.version());
    }
}
