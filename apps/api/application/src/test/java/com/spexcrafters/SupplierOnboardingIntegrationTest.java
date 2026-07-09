package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Full supplier onboarding lifecycle through the reviewer workflow (CI only; needs Docker).
 * Exercises create → draft → submit → claim → request-changes → respond → resubmit → approve,
 * the one-active-supplier invariant, cross-tenant 404 concealment, and that an org OWNER can
 * never review.
 */
class SupplierOnboardingIntegrationTest extends AbstractSupplierIntegrationTest {

    @Test
    void fullLifecycleFromDraftToApprovedActivatesSupplier() {
        TestUser owner = signUpUser();
        String organizationId = createOrganization(owner, "Acme Optics");
        JsonNode created = createSupplierApplication(owner, organizationId, "Acme Optics Manufacturing Ltd");
        String applicationId = created.get("applicationId").asText();
        String supplierId = created.get("supplierId").asText();
        assertThat(created.get("status").asText()).isEqualTo("DRAFT");
        assertThat(created.get("operationalStatus").asText()).isEqualTo("PENDING");

        int version = created.get("version").asInt();
        ResponseEntity<String> updated = patchJsonWithBearer(
                "/api/v1/suppliers/applications/" + applicationId,
                Map.of("registrationNumber", "REG-12345", "countryOfRegistration", "CN",
                        "types", java.util.List.of("LENS_MANUFACTURER"),
                        "capabilities", java.util.List.of("OEM"),
                        "version", version),
                owner.accessToken());
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> submitted = postJsonWithBearer(
                "/api/v1/suppliers/applications/" + applicationId + "/submit", Map.of(), owner.accessToken());
        assertThat(submitted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(submitted).get("status").asText()).isEqualTo("SUBMITTED");

        // Org OWNER can never review — no platform staff grant.
        ResponseEntity<String> ownerApprove = postJsonWithBearer(
                "/api/v1/platform/review/suppliers/" + applicationId + "/approve", Map.of(), owner.accessToken());
        assertThat(ownerApprove.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        TestUser reviewer = signUpUser();
        promoteToPlatformStaff(reviewer.userId(), "SENIOR_REVIEWER");

        assertThat(json(postJsonWithBearer(
                "/api/v1/platform/review/suppliers/" + applicationId + "/claim", Map.of(),
                reviewer.accessToken())).get("status").asText()).isEqualTo("UNDER_REVIEW");

        postJsonWithBearer("/api/v1/platform/review/suppliers/" + applicationId + "/request-changes",
                Map.of("requestedItem", "registration", "reason", "Please clarify the registration authority."),
                reviewer.accessToken());

        JsonNode changeRequests = json(getWithBearer(
                "/api/v1/suppliers/applications/" + applicationId + "/change-requests", owner.accessToken()));
        assertThat(changeRequests).hasSize(1);
        String changeRequestId = changeRequests.get(0).get("id").asText();

        postJsonWithBearer("/api/v1/suppliers/applications/" + applicationId
                        + "/change-requests/" + changeRequestId + "/respond",
                Map.of("response", "The authority is SAMR.", "responseLocale", "en"), owner.accessToken());

        assertThat(json(postJsonWithBearer("/api/v1/suppliers/applications/" + applicationId + "/submit",
                Map.of(), owner.accessToken())).get("status").asText()).isEqualTo("RESUBMITTED");

        postJsonWithBearer("/api/v1/platform/review/suppliers/" + applicationId + "/claim", Map.of(),
                reviewer.accessToken());
        JsonNode approved = json(postJsonWithBearer(
                "/api/v1/platform/review/suppliers/" + applicationId + "/approve", Map.of(),
                reviewer.accessToken()));
        assertThat(approved.get("status").asText()).isEqualTo("APPROVED");
        assertThat(approved.get("operationalStatus").asText()).isEqualTo("ACTIVE");

        // Audit trail for the reviewer's decisions.
        assertThat(auditActionsBy(reviewer.userId()))
                .contains("supplier.application.approved", "supplier.activated");

        // Public profile foundation is now visible for the ACTIVE supplier.
        ResponseEntity<String> publicProfile = rest.getForEntity(
                "/api/v1/public/suppliers/" + supplierId + "/profile-foundation?locale=fr", String.class);
        assertThat(publicProfile.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(publicProfile).get("legalName").asText()).isEqualTo("Acme Optics Manufacturing Ltd");
    }

    @Test
    void secondActiveSupplierForOrganizationIsRejected() {
        TestUser owner = signUpUser();
        String organizationId = createOrganization(owner, "Beta Optics");
        createSupplierApplication(owner, organizationId, "Beta Optics Ltd");
        ResponseEntity<String> second = postJsonWithBearer("/api/v1/suppliers/applications",
                Map.of("organizationId", organizationId, "originalLocale", "en", "legalName", "Beta Two"),
                owner.accessToken());
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void crossTenantAccessIsConcealedAs404() {
        TestUser owner = signUpUser();
        String organizationId = createOrganization(owner, "Gamma Optics");
        JsonNode created = createSupplierApplication(owner, organizationId, "Gamma Optics Ltd");
        String applicationId = created.get("applicationId").asText();
        String supplierId = created.get("supplierId").asText();

        TestUser outsider = signUpUser();
        assertThat(getWithBearer("/api/v1/suppliers/applications/" + applicationId, outsider.accessToken())
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getWithBearer("/api/v1/suppliers/" + supplierId + "/profile", outsider.accessToken())
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
