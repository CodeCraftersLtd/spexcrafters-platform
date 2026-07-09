package com.spexcrafters.supplier.web;

import com.spexcrafters.supplier.api.FacilityDto;
import com.spexcrafters.supplier.api.ProfileTranslationDto;
import com.spexcrafters.supplier.api.SupplierProfileDto;
import com.spexcrafters.supplier.api.SupplierProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Supplier profile reads and translation/facility management. */
@RestController
@RequestMapping("/api/v1/suppliers/{supplierId}")
public class SupplierProfileController {

    private final SupplierProfileService profileService;

    public SupplierProfileController(SupplierProfileService profileService) {
        this.profileService = profileService;
    }

    /** operationId: getSupplierProfile */
    @GetMapping("/profile")
    public SupplierProfileDto getSupplierProfile(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID supplierId) {
        return profileService.getProfile(AuthenticatedUser.id(jwt), supplierId);
    }

    /** operationId: upsertProfileTranslation */
    @PutMapping("/profile/translations/{locale}")
    public ProfileTranslationDto upsertProfileTranslation(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID supplierId, @PathVariable String locale,
            @Valid @RequestBody UpsertTranslationRequest request) {
        return profileService.upsertTranslation(AuthenticatedUser.id(jwt), supplierId, locale,
                request.toContent());
    }

    /** operationId: approveProfileTranslation */
    @PostMapping("/profile/translations/{locale}/approve")
    public ProfileTranslationDto approveProfileTranslation(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID supplierId, @PathVariable String locale) {
        return profileService.approveTranslation(AuthenticatedUser.id(jwt), supplierId, locale);
    }

    /** operationId: rejectProfileTranslation */
    @PostMapping("/profile/translations/{locale}/reject")
    public ProfileTranslationDto rejectProfileTranslation(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID supplierId, @PathVariable String locale) {
        return profileService.rejectTranslation(AuthenticatedUser.id(jwt), supplierId, locale);
    }

    /** operationId: addSupplierFacility */
    @PostMapping("/facilities")
    @ResponseStatus(HttpStatus.CREATED)
    public FacilityDto addSupplierFacility(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID supplierId, @Valid @RequestBody AddFacilityRequest request) {
        return profileService.addFacility(AuthenticatedUser.id(jwt), supplierId, request.facilityTypeCode(),
                request.country(), request.region(), request.city(), request.addressPrivacy(),
                request.ownership(), request.isPublic(), request.name(), request.description());
    }
}
