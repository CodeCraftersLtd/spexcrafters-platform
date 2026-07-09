package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

/**
 * Base for Phase-7 supplier integration tests: reuses the organizations HTTP/sign-up helpers,
 * adds a shared MinIO container wired to the evidence {@code ObjectStorage} adapter, a
 * platform-staff bootstrap helper, and a direct-PUT helper for the presigned upload leg.
 * CI only (needs Docker). Extends the shared base additively.
 */
public abstract class AbstractSupplierIntegrationTest extends AbstractOrganizationsIntegrationTest {

    static final MinIOContainer MINIO =
            new MinIOContainer("minio/minio:RELEASE.2024-08-17T01-24-54Z");

    static final String EVIDENCE_BUCKET = "spexcrafters-evidence-test";

    static {
        MINIO.start();
    }

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("spexcrafters.storage.endpoint", MINIO::getS3URL);
        registry.add("spexcrafters.storage.access-key", MINIO::getUserName);
        registry.add("spexcrafters.storage.secret-key", MINIO::getPassword);
        registry.add("spexcrafters.storage.evidence-bucket", () -> EVIDENCE_BUCKET);
        registry.add("spexcrafters.storage.path-style-access", () -> "true");
        registry.add("spexcrafters.storage.region", () -> "us-east-1");
    }

    @Autowired
    protected S3Client s3Client;

    protected void ensureEvidenceBucket() {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(EVIDENCE_BUCKET).build());
        } catch (BucketAlreadyOwnedByYouException | software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException ignored) {
            // Bucket already present from a previous test — fine.
        }
    }

    /** Bootstraps a platform-staff grant for {@code userId} (documented dev/test mechanism). */
    protected void promoteToPlatformStaff(String userId, String platformRole) {
        jdbcTemplate.update(
                "insert into platform_access.platform_staff "
                        + "(id, user_id, platform_role, active, created_at, updated_at, version) "
                        + "values (?, ?, ?, true, now(), now(), 0)",
                UUID.randomUUID(), UUID.fromString(userId), platformRole);
    }

    /** Creates a supplier application as {@code owner} for {@code organizationId}; returns the body. */
    protected JsonNode createSupplierApplication(TestUser owner, String organizationId, String legalName) {
        ResponseEntity<String> response = postJsonWithBearer("/api/v1/suppliers/applications",
                Map.of("organizationId", organizationId, "originalLocale", "en", "legalName", legalName),
                owner.accessToken());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return json(response);
    }

    /** PUTs {@code bytes} to a presigned upload URL, echoing the required content type. */
    protected int putToPresignedUrl(String url, String contentType, byte[] bytes) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<Void> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", contentType)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build(), HttpResponse.BodyHandlers.discarding());
        return response.statusCode();
    }
}
