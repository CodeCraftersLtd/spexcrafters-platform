package com.spexcrafters.supplier.web;

import com.spexcrafters.supplier.api.ReviewRequestDto;
import com.spexcrafters.supplier.api.SupplierApplicationDto;
import com.spexcrafters.supplier.api.SupplierApplicationService;
import jakarta.validation.Valid;
import java.util.List;
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
 * Supplier application resource. Thin: validation is declarative, authorization and lifecycle
 * live in {@link SupplierApplicationService}, error mapping in the shared problem handler.
 */
@RestController
@RequestMapping("/api/v1/suppliers/applications")
public class SupplierApplicationController {

    private final SupplierApplicationService applicationService;

    public SupplierApplicationController(SupplierApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /** operationId: createSupplierApplication */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierApplicationDto createSupplierApplication(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateApplicationRequest request) {
        return applicationService.create(AuthenticatedUser.id(jwt), request.organizationId(),
                request.originalLocale(), request.legalName());
    }

    /** operationId: getSupplierApplication */
    @GetMapping("/{applicationId}")
    public SupplierApplicationDto getSupplierApplication(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID applicationId) {
        return applicationService.get(AuthenticatedUser.id(jwt), applicationId);
    }

    /** operationId: updateSupplierApplicationDraft */
    @PatchMapping("/{applicationId}")
    public SupplierApplicationDto updateSupplierApplicationDraft(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID applicationId, @Valid @RequestBody UpdateDraftRequest request) {
        return applicationService.updateDraft(AuthenticatedUser.id(jwt), applicationId,
                request.toDraftUpdate(), request.version());
    }

    /** operationId: submitSupplierApplication */
    @PostMapping("/{applicationId}/submit")
    public SupplierApplicationDto submitSupplierApplication(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID applicationId) {
        return applicationService.submit(AuthenticatedUser.id(jwt), applicationId);
    }

    /** operationId: withdrawSupplierApplication */
    @PostMapping("/{applicationId}/withdraw")
    public SupplierApplicationDto withdrawSupplierApplication(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID applicationId) {
        return applicationService.withdraw(AuthenticatedUser.id(jwt), applicationId);
    }

    /** operationId: listChangeRequests */
    @GetMapping("/{applicationId}/change-requests")
    public List<ReviewRequestDto> listChangeRequests(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID applicationId) {
        return applicationService.listChangeRequests(AuthenticatedUser.id(jwt), applicationId);
    }

    /** operationId: respondToChangeRequest */
    @PostMapping("/{applicationId}/change-requests/{changeRequestId}/respond")
    public ReviewRequestDto respondToChangeRequest(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID applicationId, @PathVariable UUID changeRequestId,
            @Valid @RequestBody RespondChangeRequestRequest request) {
        return applicationService.respondToChangeRequest(AuthenticatedUser.id(jwt), applicationId,
                changeRequestId, request.response(), request.responseLocale());
    }
}
