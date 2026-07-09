package com.spexcrafters.supplier.web;

import com.spexcrafters.supplier.api.ReviewDetailDto;
import com.spexcrafters.supplier.api.ReviewQueuePageDto;
import com.spexcrafters.supplier.api.SupplierReviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform-staff reviewer resource. Every operation is authorized against a platform
 * capability in {@link SupplierReviewService} — never an organization role.
 */
@RestController
@RequestMapping("/api/v1/platform")
public class SupplierReviewController {

    private final SupplierReviewService reviewService;

    public SupplierReviewController(SupplierReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** operationId: listReviewQueue */
    @GetMapping("/review/suppliers")
    public ReviewQueuePageDto listReviewQueue(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size) {
        return reviewService.queue(AuthenticatedUser.id(jwt), cursor, size);
    }

    /** operationId: getReviewDetail */
    @GetMapping("/review/suppliers/{applicationId}")
    public ReviewDetailDto getReviewDetail(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID applicationId) {
        return reviewService.detail(AuthenticatedUser.id(jwt), applicationId);
    }

    /** operationId: claimReview */
    @PostMapping("/review/suppliers/{applicationId}/claim")
    public ReviewDetailDto claimReview(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId) {
        return reviewService.claim(AuthenticatedUser.id(jwt), applicationId);
    }

    /** operationId: requestSupplierChanges */
    @PostMapping("/review/suppliers/{applicationId}/request-changes")
    public ReviewDetailDto requestSupplierChanges(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID applicationId, @Valid @RequestBody RequestChangesRequest request) {
        return reviewService.requestChanges(AuthenticatedUser.id(jwt), applicationId,
                request.requestedItem(), request.reason());
    }

    /** operationId: approveSupplierApplication */
    @PostMapping("/review/suppliers/{applicationId}/approve")
    public ReviewDetailDto approveSupplierApplication(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID applicationId) {
        return reviewService.approve(AuthenticatedUser.id(jwt), applicationId);
    }

    /** operationId: rejectSupplierApplication */
    @PostMapping("/review/suppliers/{applicationId}/reject")
    public ReviewDetailDto rejectSupplierApplication(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID applicationId, @RequestBody(required = false) ReasonRequest request) {
        return reviewService.reject(AuthenticatedUser.id(jwt), applicationId,
                request == null ? null : request.reasonOrNull());
    }

    /** operationId: suspendSupplier */
    @PostMapping("/suppliers/{supplierId}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspendSupplier(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID supplierId,
            @RequestBody(required = false) ReasonRequest request) {
        reviewService.suspendSupplier(AuthenticatedUser.id(jwt), supplierId,
                request == null ? null : request.reasonOrNull());
    }
}
