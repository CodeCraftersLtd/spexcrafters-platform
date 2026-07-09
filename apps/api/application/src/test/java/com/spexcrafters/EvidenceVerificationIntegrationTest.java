package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Evidence upload/finalize/download authorization against MinIO, plus scope-based verification
 * grant/suspend/revoke by platform staff (CI only; needs Docker). Covers disallowed type,
 * checksum mismatch, unsafe filename → safe key, cross-tenant IDOR concealment, reviewer
 * download, deletion, and the evidence-linkage invariant on a grant.
 */
class EvidenceVerificationIntegrationTest extends AbstractSupplierIntegrationTest {

    private static final byte[] PDF_BYTES = "%PDF-1.7\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF".getBytes();

    @BeforeEach
    void setUp() {
        ensureEvidenceBucket();
    }

    private record Fixture(TestUser owner, String organizationId, String supplierId) {
    }

    private Fixture newSupplier(String name) {
        TestUser owner = signUpUser();
        String organizationId = createOrganization(owner, name);
        JsonNode created = createSupplierApplication(owner, organizationId, name + " Ltd");
        return new Fixture(owner, organizationId, created.get("supplierId").asText());
    }

    private String uploadEvidence(Fixture fixture, String filename) throws Exception {
        ResponseEntity<String> initiated = postJsonWithBearer(
                "/api/v1/suppliers/" + fixture.supplierId() + "/evidence/initiate-upload",
                Map.of("evidenceTypeCode", "BUSINESS_REGISTRATION_DOCUMENT", "filename", filename,
                        "mediaType", "application/pdf"),
                fixture.owner().accessToken());
        assertThat(initiated.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode ticket = json(initiated);
        String evidenceId = ticket.get("evidenceId").asText();
        assertThat(putToPresignedUrl(ticket.get("url").asText(), "application/pdf", PDF_BYTES)).isEqualTo(200);
        ResponseEntity<String> finalized = postJsonWithBearer(
                "/api/v1/suppliers/" + fixture.supplierId() + "/evidence/" + evidenceId + "/finalize",
                Map.of(), fixture.owner().accessToken());
        assertThat(finalized.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(finalized).get("scanStatus").asText()).isEqualTo("PENDING_SCAN");
        assertThat(json(finalized).get("downloadable").asBoolean()).isTrue();
        return evidenceId;
    }

    @Test
    void uploadFinalizeAndDownloadWithSafeKeyFromUnsafeFilename() throws Exception {
        Fixture fixture = newSupplier("Delta Optics");
        String evidenceId = uploadEvidence(fixture, "../../etc/passwd.pdf");

        JsonNode listed = json(getWithBearer(
                "/api/v1/suppliers/" + fixture.supplierId() + "/evidence", fixture.owner().accessToken()));
        assertThat(listed).hasSize(1);
        assertThat(listed.get(0).get("originalFilename").asText()).isEqualTo("passwd.pdf");

        ResponseEntity<byte[]> download = rest.exchange(
                "/api/v1/suppliers/" + fixture.supplierId() + "/evidence/" + evidenceId + "/download",
                org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(bearer(fixture.owner())), byte[].class);
        assertThat(download.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(download.getBody()).isEqualTo(PDF_BYTES);
    }

    @Test
    void disallowedMediaTypeIsRejectedAtInitiate() {
        Fixture fixture = newSupplier("Epsilon Optics");
        ResponseEntity<String> initiated = postJsonWithBearer(
                "/api/v1/suppliers/" + fixture.supplierId() + "/evidence/initiate-upload",
                Map.of("evidenceTypeCode", "BUSINESS_REGISTRATION_DOCUMENT", "filename", "malware.zip",
                        "mediaType", "application/zip"),
                fixture.owner().accessToken());
        assertThat(initiated.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void checksumMismatchOnFinalizeIsRejected() throws Exception {
        Fixture fixture = newSupplier("Zeta Optics");
        ResponseEntity<String> initiated = postJsonWithBearer(
                "/api/v1/suppliers/" + fixture.supplierId() + "/evidence/initiate-upload",
                Map.of("evidenceTypeCode", "BUSINESS_REGISTRATION_DOCUMENT", "filename", "reg.pdf",
                        "mediaType", "application/pdf"),
                fixture.owner().accessToken());
        JsonNode ticket = json(initiated);
        String evidenceId = ticket.get("evidenceId").asText();
        putToPresignedUrl(ticket.get("url").asText(), "application/pdf", PDF_BYTES);
        ResponseEntity<String> finalized = postJsonWithBearer(
                "/api/v1/suppliers/" + fixture.supplierId() + "/evidence/" + evidenceId + "/finalize",
                Map.of("expectedSha256", "0".repeat(64)), fixture.owner().accessToken());
        assertThat(finalized.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void crossTenantEvidenceDownloadIsConcealed() throws Exception {
        Fixture fixture = newSupplier("Eta Optics");
        String evidenceId = uploadEvidence(fixture, "reg.pdf");
        TestUser outsider = signUpUser();
        assertThat(getWithBearer(
                "/api/v1/suppliers/" + fixture.supplierId() + "/evidence/" + evidenceId + "/download",
                outsider.accessToken()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deletedEvidenceIsUnavailable() throws Exception {
        Fixture fixture = newSupplier("Theta Optics");
        String evidenceId = uploadEvidence(fixture, "reg.pdf");
        assertThat(deleteWithBearer(
                "/api/v1/suppliers/" + fixture.supplierId() + "/evidence/" + evidenceId,
                fixture.owner().accessToken()).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(getWithBearer(
                "/api/v1/suppliers/" + fixture.supplierId() + "/evidence/" + evidenceId + "/download",
                fixture.owner().accessToken()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void reviewerCanDownloadAndGrantSuspendRevokeVerificationScope() throws Exception {
        Fixture fixture = newSupplier("Iota Optics");
        String evidenceId = uploadEvidence(fixture, "reg.pdf");

        TestUser reviewer = signUpUser();
        promoteToPlatformStaff(reviewer.userId(), "SENIOR_REVIEWER");

        // Reviewer download (audited).
        assertThat(getWithBearer(
                "/api/v1/suppliers/" + fixture.supplierId() + "/evidence/" + evidenceId + "/download",
                reviewer.accessToken()).getStatusCode()).isEqualTo(HttpStatus.OK);

        // Non-staff grant is denied.
        assertThat(postJsonWithBearer(
                "/api/v1/platform/suppliers/" + fixture.supplierId()
                        + "/verification/scopes/BUSINESS_REGISTRATION/grant",
                Map.of("evidenceIds", List.of(evidenceId)), fixture.owner().accessToken())
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Staff grant with evidence linkage.
        JsonNode granted = json(postJsonWithBearer(
                "/api/v1/platform/suppliers/" + fixture.supplierId()
                        + "/verification/scopes/BUSINESS_REGISTRATION/grant",
                Map.of("evidenceIds", List.of(evidenceId), "reason", "Registration verified"),
                reviewer.accessToken()));
        assertThat(scopeStatus(granted, "BUSINESS_REGISTRATION")).isEqualTo("VERIFIED");

        // Org read of verification status.
        JsonNode status = json(getWithBearer(
                "/api/v1/suppliers/" + fixture.supplierId() + "/verification", fixture.owner().accessToken()));
        assertThat(scopeStatus(status, "BUSINESS_REGISTRATION")).isEqualTo("VERIFIED");

        // Suspend then revoke.
        JsonNode suspended = json(postJsonWithBearer(
                "/api/v1/platform/suppliers/" + fixture.supplierId()
                        + "/verification/scopes/BUSINESS_REGISTRATION/suspend",
                Map.of("reason", "Under review"), reviewer.accessToken()));
        assertThat(scopeStatus(suspended, "BUSINESS_REGISTRATION")).isEqualTo("SUSPENDED");

        JsonNode revoked = json(postJsonWithBearer(
                "/api/v1/platform/suppliers/" + fixture.supplierId()
                        + "/verification/scopes/BUSINESS_REGISTRATION/revoke",
                Map.of("reason", "Fraud"), reviewer.accessToken()));
        assertThat(scopeStatus(revoked, "BUSINESS_REGISTRATION")).isEqualTo("REVOKED");

        assertThat(auditActionsBy(reviewer.userId())).contains(
                "supplier.verification.granted", "supplier.verification.suspended",
                "supplier.verification.revoked");
    }

    @Test
    void grantWithoutEvidenceLinkageIsRejected() {
        Fixture fixture = newSupplier("Kappa Optics");
        TestUser reviewer = signUpUser();
        promoteToPlatformStaff(reviewer.userId(), "SENIOR_REVIEWER");
        ResponseEntity<String> response = postJsonWithBearer(
                "/api/v1/platform/suppliers/" + fixture.supplierId()
                        + "/verification/scopes/LEGAL_ENTITY/grant",
                Map.of("evidenceIds", List.of()), reviewer.accessToken());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private static String scopeStatus(JsonNode status, String scopeCode) {
        for (JsonNode scope : status.get("scopes")) {
            if (scope.get("scopeCode").asText().equals(scopeCode)) {
                return scope.get("status").asText();
            }
        }
        return null;
    }

    private org.springframework.http.HttpHeaders bearer(TestUser user) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(user.accessToken());
        return headers;
    }
}
