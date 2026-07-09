package com.spexcrafters.media.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * S3-compatible object storage configuration (ADR-023). Local/CI point this at MinIO with
 * non-production credentials and an isolated private bucket; production points it at AWS S3
 * with a private, server-side-encrypted bucket. No production credentials live here.
 *
 * @param endpoint        S3 endpoint (MinIO: {@code http://localhost:9000}); blank uses the
 *                        AWS default endpoint for {@code region}
 * @param region          AWS region; MinIO ignores it but the SDK requires a value
 * @param accessKey       access key id
 * @param secretKey       secret access key
 * @param evidenceBucket  the private bucket holding verification evidence
 * @param pathStyleAccess path-style addressing (required by MinIO; harmless on S3)
 * @param presignTtl      lifetime of presigned upload URLs (short-lived)
 */
@ConfigurationProperties(prefix = "spexcrafters.storage")
public record S3StorageProperties(
        String endpoint,
        String region,
        String accessKey,
        String secretKey,
        String evidenceBucket,
        boolean pathStyleAccess,
        java.time.Duration presignTtl) {

    public S3StorageProperties {
        if (region == null || region.isBlank()) {
            region = "us-east-1";
        }
        if (evidenceBucket == null || evidenceBucket.isBlank()) {
            evidenceBucket = "spexcrafters-evidence";
        }
        if (presignTtl == null) {
            presignTtl = java.time.Duration.ofMinutes(10);
        }
    }
}
