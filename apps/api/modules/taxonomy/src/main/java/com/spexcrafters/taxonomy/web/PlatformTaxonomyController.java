package com.spexcrafters.taxonomy.web;

import com.spexcrafters.taxonomy.api.AttributeDetail;
import com.spexcrafters.taxonomy.api.AttributeService;
import com.spexcrafters.taxonomy.api.BrandDetail;
import com.spexcrafters.taxonomy.api.BrandService;
import com.spexcrafters.taxonomy.api.CategoryDetail;
import com.spexcrafters.taxonomy.api.CategoryService;
import com.spexcrafters.taxonomy.api.Certification;
import com.spexcrafters.taxonomy.api.CertificationService;
import com.spexcrafters.taxonomy.api.EffectiveSpecificationTemplate;
import com.spexcrafters.taxonomy.api.EnumerationDetail;
import com.spexcrafters.taxonomy.api.EnumerationService;
import com.spexcrafters.taxonomy.api.EnumerationValueView;
import com.spexcrafters.taxonomy.api.TranslationView;
import com.spexcrafters.taxonomy.api.Unit;
import com.spexcrafters.taxonomy.api.UnitService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform-staff taxonomy administration (18 operations). Every operation is authorized against
 * a platform capability in the service layer ({@code TAXONOMY_WRITE}, or {@code BRAND_APPROVE}
 * for brand approval) — never an organization role. Non-staff callers receive a 403.
 */
@RestController
@RequestMapping("/api/v1/platform/taxonomy")
public class PlatformTaxonomyController {

    private final CategoryService categoryService;
    private final AttributeService attributeService;
    private final EnumerationService enumerationService;
    private final UnitService unitService;
    private final CertificationService certificationService;
    private final BrandService brandService;

    public PlatformTaxonomyController(CategoryService categoryService, AttributeService attributeService,
            EnumerationService enumerationService, UnitService unitService,
            CertificationService certificationService, BrandService brandService) {
        this.categoryService = categoryService;
        this.attributeService = attributeService;
        this.enumerationService = enumerationService;
        this.unitService = unitService;
        this.certificationService = certificationService;
        this.brandService = brandService;
    }

    // ------------------------------------------------------------------ categories

    /** operationId: createCategory */
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDetail createCategory(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.create(AuthenticatedUser.id(jwt), request.code(), request.parentCode(),
                request.classification(), request.originalLocale(), request.name(), request.sortOrder());
    }

    /** operationId: updateCategory */
    @PatchMapping("/categories/{id}")
    public CategoryDetail updateCategory(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return categoryService.update(AuthenticatedUser.id(jwt), id, request.parentCode(),
                request.classification(), request.sortOrder(), request.version());
    }

    /** operationId: setCategoryActivation */
    @PostMapping("/categories/{id}/activation")
    public CategoryDetail setCategoryActivation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody ActivationRequest request) {
        return categoryService.setActivation(AuthenticatedUser.id(jwt), id, request.active());
    }

    /** operationId: upsertCategoryTranslation */
    @PutMapping("/categories/{id}/translations/{locale}")
    public TranslationView upsertCategoryTranslation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @PathVariable String locale, @Valid @RequestBody TranslationUpsertRequest request) {
        return categoryService.upsertTranslation(AuthenticatedUser.id(jwt), id, locale, request.name(),
                request.description(), request.source());
    }

    /** operationId: approveCategoryTranslation */
    @PostMapping("/categories/{id}/translations/{locale}/approve")
    public TranslationView approveCategoryTranslation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @PathVariable String locale) {
        return categoryService.approveTranslation(AuthenticatedUser.id(jwt), id, locale);
    }

    /** operationId: putSpecificationTemplate */
    @PutMapping("/categories/{id}/specification-template")
    public EffectiveSpecificationTemplate putSpecificationTemplate(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id, @Valid @RequestBody PutSpecificationTemplateRequest request) {
        return categoryService.putSpecificationTemplate(AuthenticatedUser.id(jwt), id, request.code(),
                request.toInputs());
    }

    // ------------------------------------------------------------------ attributes

    /** operationId: createAttribute */
    @PostMapping("/attributes")
    @ResponseStatus(HttpStatus.CREATED)
    public AttributeDetail createAttribute(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAttributeRequest request) {
        return attributeService.create(AuthenticatedUser.id(jwt), request.toInput());
    }

    /** operationId: updateAttribute */
    @PatchMapping("/attributes/{id}")
    public AttributeDetail updateAttribute(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody UpdateAttributeRequest request) {
        return attributeService.update(AuthenticatedUser.id(jwt), id, request.toInput(), request.version());
    }

    /** operationId: setAttributeDeprecation */
    @PostMapping("/attributes/{id}/deprecation")
    public AttributeDetail setAttributeDeprecation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody DeprecationRequest request) {
        return attributeService.setDeprecation(AuthenticatedUser.id(jwt), id, request.deprecated());
    }

    /** operationId: upsertAttributeTranslation */
    @PutMapping("/attributes/{id}/translations/{locale}")
    public TranslationView upsertAttributeTranslation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @PathVariable String locale, @Valid @RequestBody TranslationUpsertRequest request) {
        return attributeService.upsertTranslation(AuthenticatedUser.id(jwt), id, locale, request.name(),
                request.description(), request.source());
    }

    // ------------------------------------------------------------------ enumerations

    /** operationId: createEnumeration */
    @PostMapping("/enumerations")
    @ResponseStatus(HttpStatus.CREATED)
    public EnumerationDetail createEnumeration(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateEnumerationRequest request) {
        return enumerationService.create(AuthenticatedUser.id(jwt), request.code());
    }

    /** operationId: addEnumerationValue */
    @PostMapping("/enumerations/{id}/values")
    @ResponseStatus(HttpStatus.CREATED)
    public EnumerationValueView addEnumerationValue(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody AddEnumerationValueRequest request) {
        return enumerationService.addValue(AuthenticatedUser.id(jwt), id, request.code(), request.sortOrder(),
                request.originalLocale(), request.label(), request.description());
    }

    /** operationId: upsertEnumerationValueTranslation */
    @PutMapping("/enumeration-values/{id}/translations/{locale}")
    public TranslationView upsertEnumerationValueTranslation(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id, @PathVariable String locale,
            @Valid @RequestBody TranslationUpsertRequest request) {
        return enumerationService.upsertValueTranslation(AuthenticatedUser.id(jwt), id, locale, request.name(),
                request.description(), request.source());
    }

    // ------------------------------------------------------------------ brands

    /** operationId: createBrand */
    @PostMapping("/brands")
    @ResponseStatus(HttpStatus.CREATED)
    public BrandDetail createBrand(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateBrandRequest request) {
        return brandService.create(AuthenticatedUser.id(jwt), request.code(), request.brandType(),
                request.canonicalName(), request.ownerCompany(), request.manufacturer(), request.countryCode(),
                request.website(), request.originalLocale(), request.displayName());
    }

    /** operationId: setBrandApproval */
    @PostMapping("/brands/{id}/approval")
    public BrandDetail setBrandApproval(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody BrandApprovalRequest request) {
        return brandService.setApproval(AuthenticatedUser.id(jwt), id, request.status());
    }

    /** operationId: upsertBrandTranslation */
    @PutMapping("/brands/{id}/translations/{locale}")
    public TranslationView upsertBrandTranslation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @PathVariable String locale, @Valid @RequestBody TranslationUpsertRequest request) {
        return brandService.upsertTranslation(AuthenticatedUser.id(jwt), id, locale, request.name(),
                request.description(), request.source());
    }

    // ------------------------------------------------------------------ certifications & units

    /** operationId: createCertification */
    @PostMapping("/certifications")
    @ResponseStatus(HttpStatus.CREATED)
    public Certification createCertification(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCertificationRequest request) {
        return certificationService.create(AuthenticatedUser.id(jwt), request.code(), request.category(),
                request.countryScope(), request.industryScope(), request.validityMonths(),
                request.originalLocale(), request.name(), request.description());
    }

    /** operationId: createUnit */
    @PostMapping("/units")
    @ResponseStatus(HttpStatus.CREATED)
    public Unit createUnit(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateUnitRequest request) {
        return unitService.create(AuthenticatedUser.id(jwt), request.code(), request.family(),
                request.baseUnitCode(), request.factorToBase(), request.offsetToBase(),
                request.displayFormat(), request.originalLocale(), request.displayName());
    }
}
