package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Full optical-taxonomy registry slice (CI only; needs Docker). Exercises the public localized
 * reads over the V5 seed, effective-template inheritance, the specification validation engine,
 * platform-staff administration with capability authorization, and the translation lifecycle
 * (upsert → approve → source-version bump makes a prior translation stale).
 */
class TaxonomyRegistryIntegrationTest extends AbstractOrganizationsIntegrationTest {

    private void promoteToPlatformStaff(String userId, String role) {
        jdbcTemplate.update(
                "insert into platform_access.platform_staff "
                        + "(id, user_id, platform_role, active, created_at, updated_at, version) "
                        + "values (?, ?, ?, true, now(), now(), 0)",
                UUID.randomUUID(), UUID.fromString(userId), role);
    }

    // --------------------------------------------------------------- public seed reads

    @Test
    void publicReadsExposeSeedWithLocaleFallback() {
        JsonNode tree = json(rest.getForEntity("/api/v1/taxonomy/categories", String.class));
        assertThat(tree.isArray()).isTrue();
        JsonNode optical = stream(tree).filter(n -> n.get("code").asText().equals("OPTICAL")).findFirst()
                .orElseThrow();
        assertThat(stream(optical.get("children")).map(c -> c.get("code").asText()))
                .contains("LENS", "FRAME");
        assertThat(optical.get("slug").asText()).isEqualTo("optical");

        // Unknown requested locale falls back to the en original name.
        JsonNode frame = json(rest.getForEntity("/api/v1/taxonomy/categories/FRAME?locale=fr", String.class));
        assertThat(frame.get("name").asText()).isEqualTo("Frame");

        JsonNode units = json(rest.getForEntity("/api/v1/taxonomy/units", String.class));
        assertThat(stream(units).filter(u -> u.get("code").asText().equals("mm")).findFirst().orElseThrow()
                .get("displayName").asText()).isEqualTo("millimetre");

        JsonNode enumeration = json(rest.getForEntity("/api/v1/taxonomy/enumerations/FRAME_SHAPE",
                String.class));
        assertThat(stream(enumeration.get("values")).map(v -> v.get("code").asText())).contains("ROUND");

        JsonNode eye = json(rest.getForEntity("/api/v1/taxonomy/attributes/EYE_SIZE", String.class));
        assertThat(eye.get("dataType").asText()).isEqualTo("MEASUREMENT");
        assertThat(eye.get("unitCode").asText()).isEqualTo("mm");
    }

    @Test
    void effectiveTemplateIncludesInheritedAncestorAttributes() {
        TestUser admin = platformAdmin();
        // A child category under FRAME has no own template, so its effective template is inherited.
        ResponseEntity<String> created = postJsonWithBearer("/api/v1/platform/taxonomy/categories",
                Map.of("code", "SPORT_FRAME", "parentCode", "FRAME", "classification", "FRAME",
                        "originalLocale", "en", "name", "Sport Frame"), admin.accessToken());
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode template = json(rest.getForEntity(
                "/api/v1/taxonomy/categories/SPORT_FRAME/specification-template", String.class));
        assertThat(template.get("categoryCode").asText()).isEqualTo("SPORT_FRAME");
        List<JsonNode> attrs = stream(template.get("attributes")).toList();
        assertThat(attrs).isNotEmpty();
        assertThat(attrs).allMatch(a -> a.get("inherited").asBoolean());
        assertThat(attrs).allMatch(a -> a.get("sourceCategoryCode").asText().equals("FRAME"));
        assertThat(stream(template.get("attributes")).map(a -> a.get("attributeCode").asText()))
                .contains("EYE_SIZE", "BRIDGE_WIDTH", "FRAME_MATERIAL");
    }

    @Test
    void validateSpecificationHappyAndViolation() {
        Map<String, Object> valid = Map.of("categoryCode", "FRAME",
                "values", Map.of("EYE_SIZE", "48", "BRIDGE_WIDTH", "18", "FRAME_MATERIAL", "ACETATE"));
        JsonNode ok = json(postJson("/api/v1/taxonomy/specifications/validate", valid));
        assertThat(ok.get("valid").asBoolean()).isTrue();
        assertThat(ok.get("violations")).isEmpty();

        Map<String, Object> invalid = Map.of("categoryCode", "FRAME",
                "values", Map.of("EYE_SIZE", "999", "FRAME_SHAPE", "NOT_A_SHAPE"));
        JsonNode bad = json(postJson("/api/v1/taxonomy/specifications/validate", invalid));
        assertThat(bad.get("valid").asBoolean()).isFalse();
        assertThat(stream(bad.get("violations")))
                .anyMatch(v -> v.get("attributeCode").asText().equals("EYE_SIZE")
                        && v.get("code").asText().equals("out-of-range"))
                .anyMatch(v -> v.get("attributeCode").asText().equals("FRAME_SHAPE")
                        && v.get("code").asText().equals("not-a-member"))
                .anyMatch(v -> v.get("attributeCode").asText().equals("BRIDGE_WIDTH")
                        && v.get("code").asText().equals("required"));
    }

    // --------------------------------------------------------------- administration & authz

    @Test
    void administrationRequiresPlatformCapability() {
        TestUser outsider = signUpUser();
        ResponseEntity<String> forbidden = postJsonWithBearer("/api/v1/platform/taxonomy/categories",
                Map.of("code", "NOPE", "classification", "OTHER", "originalLocale", "en", "name", "Nope"),
                outsider.accessToken());
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void slugsAreUniquePerLocaleAndDuplicateCodeConflicts() {
        TestUser admin = platformAdmin();
        JsonNode first = json(postJsonWithBearer("/api/v1/platform/taxonomy/categories",
                Map.of("code", "SUN_A", "classification", "SUNGLASSES", "originalLocale", "en",
                        "name", "Sun Shades"), admin.accessToken()));
        JsonNode second = json(postJsonWithBearer("/api/v1/platform/taxonomy/categories",
                Map.of("code", "SUN_B", "classification", "SUNGLASSES", "originalLocale", "en",
                        "name", "Sun Shades"), admin.accessToken()));
        assertThat(first.get("primarySlug").asText()).isEqualTo("sun-shades");
        assertThat(second.get("primarySlug").asText()).isEqualTo("sun-shades-2");

        ResponseEntity<String> dupCode = postJsonWithBearer("/api/v1/platform/taxonomy/categories",
                Map.of("code", "SUN_A", "classification", "SUNGLASSES", "originalLocale", "en", "name", "Dup"),
                admin.accessToken());
        assertThat(dupCode.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void translationLifecycleBumpMakesPriorTranslationStale() {
        TestUser admin = platformAdmin();
        JsonNode created = json(postJsonWithBearer("/api/v1/platform/taxonomy/categories",
                Map.of("code", "RIM_CAT", "classification", "FRAME", "originalLocale", "en", "name", "Rim"),
                admin.accessToken()));
        String id = created.get("id").asText();

        JsonNode fr = json(putJsonWithBearer("/api/v1/platform/taxonomy/categories/" + id + "/translations/fr",
                Map.of("name", "Cercle", "source", "HUMAN"), admin.accessToken()));
        assertThat(fr.get("isOriginal").asBoolean()).isFalse();
        assertThat(fr.get("translationStatus").asText()).isEqualTo("DRAFT");
        assertThat(fr.get("stale").asBoolean()).isFalse();

        JsonNode approved = json(postJsonWithBearer(
                "/api/v1/platform/taxonomy/categories/" + id + "/translations/fr/approve", Map.of(),
                admin.accessToken()));
        assertThat(approved.get("translationStatus").asText()).isEqualTo("APPROVED");

        // Editing the original (en) content bumps the category source version.
        putJsonWithBearer("/api/v1/platform/taxonomy/categories/" + id + "/translations/en",
                Map.of("name", "Rim Updated", "source", "HUMAN"), admin.accessToken());

        // Re-approving fr (whose source version now lags) reports it stale.
        JsonNode restale = json(postJsonWithBearer(
                "/api/v1/platform/taxonomy/categories/" + id + "/translations/fr/approve", Map.of(),
                admin.accessToken()));
        assertThat(restale.get("stale").asBoolean()).isTrue();
    }

    @Test
    void createEnumerationUnitCertificationAndBrandApprovalGatesPublicVisibility() {
        TestUser admin = platformAdmin();

        assertThat(postJsonWithBearer("/api/v1/platform/taxonomy/units",
                Map.of("code", "furlong", "family", "LENGTH", "baseUnitCode", "mm", "factorToBase", 201168,
                        "offsetToBase", 0, "originalLocale", "en", "displayName", "furlong"),
                admin.accessToken()).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode enumeration = json(postJsonWithBearer("/api/v1/platform/taxonomy/enumerations",
                Map.of("code", "TEST_ENUM"), admin.accessToken()));
        assertThat(enumeration.get("code").asText()).isEqualTo("TEST_ENUM");

        assertThat(postJsonWithBearer("/api/v1/platform/taxonomy/certifications",
                Map.of("code", "TEST_CERT", "category", "QUALITY", "originalLocale", "en",
                        "name", "Test Certification"), admin.accessToken()).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        JsonNode brand = json(postJsonWithBearer("/api/v1/platform/taxonomy/brands",
                Map.of("code", "TESTBRAND", "brandType", "FRAME", "canonicalName", "Test Brand",
                        "originalLocale", "en", "displayName", "Test Brand"), admin.accessToken()));
        assertThat(brand.get("approvalStatus").asText()).isEqualTo("PENDING");
        String brandId = brand.get("id").asText();

        // Pending brand is invisible to the public.
        assertThat(rest.getForEntity("/api/v1/taxonomy/brands/TESTBRAND", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        JsonNode approved = json(postJsonWithBearer("/api/v1/platform/taxonomy/brands/" + brandId + "/approval",
                Map.of("status", "APPROVED"), admin.accessToken()));
        assertThat(approved.get("approvalStatus").asText()).isEqualTo("APPROVED");

        assertThat(rest.getForEntity("/api/v1/taxonomy/brands/TESTBRAND", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    // --------------------------------------------------------------- helpers

    private TestUser platformAdmin() {
        TestUser admin = signUpUser();
        promoteToPlatformStaff(admin.userId(), "PLATFORM_ADMIN");
        return admin;
    }

    private static java.util.stream.Stream<JsonNode> stream(JsonNode array) {
        return array == null ? java.util.stream.Stream.empty()
                : java.util.stream.StreamSupport.stream(array.spliterator(), false);
    }
}
