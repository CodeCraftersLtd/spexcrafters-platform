package com.spexcrafters.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enforces the module interaction rules of system-architecture §G.3:
 * a module's only public surface is its {@code ...api} package, controllers live in
 * {@code ...web}, JPA entities never cross into the web layer, and the module graph is
 * acyclic. New modules must be added to the internals rules below.
 */
@AnalyzeClasses(packages = "com.spexcrafters", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleArchitectureTest {

    private static final String[] MODULE_PACKAGES = {
            "com.spexcrafters.identity..",
            "com.spexcrafters.audit..",
            "com.spexcrafters.organizations..",
            "com.spexcrafters.media..",
            "com.spexcrafters.platformaccess..",
            "com.spexcrafters.supplier..",
            "com.spexcrafters.verification..",
    };

    // ------------------------------------------------- cross-module access only via ..api..

    @ArchTest
    static final ArchRule identity_internals_are_module_private = noClasses()
            .that().resideOutsideOfPackage("com.spexcrafters.identity..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.spexcrafters.identity.domain..",
                    "com.spexcrafters.identity.infrastructure..",
                    "com.spexcrafters.identity.web..");

    @ArchTest
    static final ArchRule audit_internals_are_module_private = noClasses()
            .that().resideOutsideOfPackage("com.spexcrafters.audit..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.spexcrafters.audit.domain..",
                    "com.spexcrafters.audit.infrastructure..",
                    "com.spexcrafters.audit.web..");

    @ArchTest
    static final ArchRule organizations_internals_are_module_private = noClasses()
            .that().resideOutsideOfPackage("com.spexcrafters.organizations..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.spexcrafters.organizations.domain..",
                    "com.spexcrafters.organizations.infrastructure..",
                    "com.spexcrafters.organizations.web..");

    @ArchTest
    static final ArchRule media_internals_are_module_private = noClasses()
            .that().resideOutsideOfPackage("com.spexcrafters.media..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.spexcrafters.media.infrastructure..");

    @ArchTest
    static final ArchRule platform_access_internals_are_module_private = noClasses()
            .that().resideOutsideOfPackage("com.spexcrafters.platformaccess..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.spexcrafters.platformaccess.domain..",
                    "com.spexcrafters.platformaccess.infrastructure..");

    @ArchTest
    static final ArchRule supplier_internals_are_module_private = noClasses()
            .that().resideOutsideOfPackage("com.spexcrafters.supplier..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.spexcrafters.supplier.domain..",
                    "com.spexcrafters.supplier.infrastructure..",
                    "com.spexcrafters.supplier.web..");

    @ArchTest
    static final ArchRule verification_internals_are_module_private = noClasses()
            .that().resideOutsideOfPackage("com.spexcrafters.verification..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.spexcrafters.verification.domain..",
                    "com.spexcrafters.verification.infrastructure..",
                    "com.spexcrafters.verification.web..");

    /** The supplier context consumes other modules strictly through their {@code api} packages. */
    @ArchTest
    static final ArchRule supplier_uses_other_modules_only_via_their_api = noClasses()
            .that().resideInAPackage("com.spexcrafters.supplier..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.spexcrafters.identity.domain..", "com.spexcrafters.identity.infrastructure..",
                    "com.spexcrafters.identity.web..",
                    "com.spexcrafters.organizations.domain..", "com.spexcrafters.organizations.infrastructure..",
                    "com.spexcrafters.organizations.web..",
                    "com.spexcrafters.platformaccess.domain..",
                    "com.spexcrafters.platformaccess.infrastructure..",
                    "com.spexcrafters.media.infrastructure..");

    /** The verification context consumes the supplier and platform-access modules via their api only. */
    @ArchTest
    static final ArchRule verification_uses_other_modules_only_via_their_api = noClasses()
            .that().resideInAPackage("com.spexcrafters.verification..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.spexcrafters.supplier.domain..", "com.spexcrafters.supplier.infrastructure..",
                    "com.spexcrafters.supplier.web..",
                    "com.spexcrafters.platformaccess.domain..",
                    "com.spexcrafters.platformaccess.infrastructure..");

    // ---------------------------------------------- identity ↔ organizations interaction

    /**
     * Organizations consumes identity strictly through {@code identity.api}
     * (UserDirectory/UserSummaryDto) — never its domain model, repositories or web layer.
     * Subsumed by the internals rules above, but pinned explicitly for the module pair
     * that actually interacts.
     */
    @ArchTest
    static final ArchRule organizations_uses_identity_only_via_its_api = noClasses()
            .that().resideInAPackage("com.spexcrafters.organizations..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.spexcrafters.identity.domain..",
                    "com.spexcrafters.identity.infrastructure..",
                    "com.spexcrafters.identity.web..");

    /** The dependency is one-directional: identity never knows about organizations. */
    @ArchTest
    static final ArchRule identity_never_depends_on_organizations = noClasses()
            .that().resideInAPackage("com.spexcrafters.identity..")
            .should().dependOnClassesThat().resideInAPackage("com.spexcrafters.organizations..");

    // -------------------------------------------------------------- shared-kernel is a leaf

    @ArchTest
    static final ArchRule shared_kernel_depends_on_no_module = noClasses()
            .that().resideInAPackage("com.spexcrafters.sharedkernel..")
            .should().dependOnClassesThat().resideInAnyPackage(MODULE_PACKAGES);

    // ------------------------------------------------------------------------ layering rules

    @ArchTest
    static final ArchRule controllers_live_only_in_web_packages = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..web..");

    @ArchTest
    static final ArchRule web_layer_never_touches_jpa_entities = noClasses()
            .that().resideInAPackage("..web..")
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class);

    // ----------------------------------------------------------------------------- no cycles

    @ArchTest
    static final ArchRule module_graph_is_acyclic = slices()
            .matching("com.spexcrafters.(*)..")
            .should().beFreeOfCycles();
}
