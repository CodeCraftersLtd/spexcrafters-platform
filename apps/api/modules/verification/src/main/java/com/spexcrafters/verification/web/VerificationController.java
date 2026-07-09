package com.spexcrafters.verification.web;

import com.spexcrafters.verification.api.VerificationService;
import com.spexcrafters.verification.api.VerificationStatusDto;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verification resource: org-scoped status reads and platform-staff scope grant/suspend/revoke.
 * Reads authorize via the supplier org-capability policy; mutations via platform capabilities.
 */
@RestController
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    /** operationId: getVerificationStatus */
    @GetMapping("/api/v1/suppliers/{supplierId}/verification")
    public VerificationStatusDto getVerificationStatus(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID supplierId) {
        return verificationService.getStatus(AuthenticatedUser.id(jwt), supplierId);
    }

    /** operationId: grantVerificationScope */
    @PostMapping("/api/v1/platform/suppliers/{supplierId}/verification/scopes/{scopeCode}/grant")
    public VerificationStatusDto grantVerificationScope(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID supplierId, @PathVariable String scopeCode,
            @Valid @RequestBody GrantScopeRequest request) {
        return verificationService.grantScope(AuthenticatedUser.id(jwt), supplierId, scopeCode,
                request.evidenceIds(), request.validUntil(), request.reason());
    }

    /** operationId: suspendVerificationScope */
    @PostMapping("/api/v1/platform/suppliers/{supplierId}/verification/scopes/{scopeCode}/suspend")
    public VerificationStatusDto suspendVerificationScope(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID supplierId, @PathVariable String scopeCode,
            @RequestBody(required = false) ScopeReasonRequest request) {
        return verificationService.suspendScope(AuthenticatedUser.id(jwt), supplierId, scopeCode,
                request == null ? null : request.reasonOrNull());
    }

    /** operationId: revokeVerificationScope */
    @PostMapping("/api/v1/platform/suppliers/{supplierId}/verification/scopes/{scopeCode}/revoke")
    public VerificationStatusDto revokeVerificationScope(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID supplierId, @PathVariable String scopeCode,
            @RequestBody(required = false) ScopeReasonRequest request) {
        return verificationService.revokeScope(AuthenticatedUser.id(jwt), supplierId, scopeCode,
                request == null ? null : request.reasonOrNull());
    }
}
