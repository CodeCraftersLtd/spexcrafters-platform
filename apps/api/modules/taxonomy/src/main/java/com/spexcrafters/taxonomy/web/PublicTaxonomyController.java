package com.spexcrafters.taxonomy.web;

import com.spexcrafters.taxonomy.api.AttributeDetail;
import com.spexcrafters.taxonomy.api.AttributeService;
import com.spexcrafters.taxonomy.api.AttributeSummary;
import com.spexcrafters.taxonomy.api.BrandDetail;
import com.spexcrafters.taxonomy.api.BrandService;
import com.spexcrafters.taxonomy.api.BrandSummary;
import com.spexcrafters.taxonomy.api.CategoryDetail;
import com.spexcrafters.taxonomy.api.CategoryService;
import com.spexcrafters.taxonomy.api.CategoryTreeNode;
import com.spexcrafters.taxonomy.api.Certification;
import com.spexcrafters.taxonomy.api.CertificationService;
import com.spexcrafters.taxonomy.api.Country;
import com.spexcrafters.taxonomy.api.CountryService;
import com.spexcrafters.taxonomy.api.EffectiveSpecificationTemplate;
import com.spexcrafters.taxonomy.api.EnumerationDetail;
import com.spexcrafters.taxonomy.api.EnumerationService;
import com.spexcrafters.taxonomy.api.EnumerationSummary;
import com.spexcrafters.taxonomy.api.SpecificationValidationResult;
import com.spexcrafters.taxonomy.api.SpecificationValidator;
import com.spexcrafters.taxonomy.api.Unit;
import com.spexcrafters.taxonomy.api.UnitService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public, unauthenticated optical-taxonomy read surface (13 operations). Exposes active,
 * displayable registry data for future modules and SEO; content resolves by the ADR-020
 * localization fallback chain.
 */
@RestController
@RequestMapping("/api/v1/taxonomy")
public class PublicTaxonomyController {

    private final CategoryService categoryService;
    private final AttributeService attributeService;
    private final EnumerationService enumerationService;
    private final UnitService unitService;
    private final CountryService countryService;
    private final CertificationService certificationService;
    private final BrandService brandService;
    private final SpecificationValidator specificationValidator;

    public PublicTaxonomyController(CategoryService categoryService, AttributeService attributeService,
            EnumerationService enumerationService, UnitService unitService, CountryService countryService,
            CertificationService certificationService, BrandService brandService,
            SpecificationValidator specificationValidator) {
        this.categoryService = categoryService;
        this.attributeService = attributeService;
        this.enumerationService = enumerationService;
        this.unitService = unitService;
        this.countryService = countryService;
        this.certificationService = certificationService;
        this.brandService = brandService;
        this.specificationValidator = specificationValidator;
    }

    /** operationId: getCategoryTree */
    @GetMapping("/categories")
    public List<CategoryTreeNode> getCategoryTree(@RequestParam(required = false) String locale) {
        return categoryService.getTree(locale);
    }

    /** operationId: getCategory */
    @GetMapping("/categories/{code}")
    public CategoryDetail getCategory(@PathVariable String code,
            @RequestParam(required = false) String locale) {
        return categoryService.getCategory(code, locale);
    }

    /** operationId: getCategorySpecificationTemplate */
    @GetMapping("/categories/{code}/specification-template")
    public EffectiveSpecificationTemplate getCategorySpecificationTemplate(@PathVariable String code,
            @RequestParam(required = false) String locale) {
        return categoryService.getEffectiveTemplate(code, locale);
    }

    /** operationId: listAttributes */
    @GetMapping("/attributes")
    public List<AttributeSummary> listAttributes(@RequestParam(required = false) String locale) {
        return attributeService.list(locale);
    }

    /** operationId: getAttribute */
    @GetMapping("/attributes/{code}")
    public AttributeDetail getAttribute(@PathVariable String code,
            @RequestParam(required = false) String locale) {
        return attributeService.get(code, locale);
    }

    /** operationId: listEnumerations */
    @GetMapping("/enumerations")
    public List<EnumerationSummary> listEnumerations(@RequestParam(required = false) String locale) {
        return enumerationService.list(locale);
    }

    /** operationId: getEnumeration */
    @GetMapping("/enumerations/{code}")
    public EnumerationDetail getEnumeration(@PathVariable String code,
            @RequestParam(required = false) String locale) {
        return enumerationService.get(code, locale);
    }

    /** operationId: listUnits */
    @GetMapping("/units")
    public List<Unit> listUnits(@RequestParam(required = false) String locale) {
        return unitService.list(locale);
    }

    /** operationId: listCountries */
    @GetMapping("/countries")
    public List<Country> listCountries(@RequestParam(required = false) String locale) {
        return countryService.list(locale);
    }

    /** operationId: listCertifications */
    @GetMapping("/certifications")
    public List<Certification> listCertifications(@RequestParam(required = false) String locale) {
        return certificationService.list(locale);
    }

    /** operationId: listBrands */
    @GetMapping("/brands")
    public List<BrandSummary> listBrands(@RequestParam(required = false) String locale,
            @RequestParam(required = false) String status) {
        return brandService.listPublic(locale);
    }

    /** operationId: getBrand */
    @GetMapping("/brands/{code}")
    public BrandDetail getBrand(@PathVariable String code,
            @RequestParam(required = false) String locale) {
        return brandService.getPublic(code, locale);
    }

    /** operationId: validateSpecification */
    @PostMapping("/specifications/validate")
    public SpecificationValidationResult validateSpecification(
            @RequestParam(required = false) String locale,
            @Valid @RequestBody SpecificationValidationRequest request) {
        return specificationValidator.validate(request.categoryCode(), request.values());
    }
}
