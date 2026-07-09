package com.spexcrafters.supplier.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Evidence upload constraints (evidence-storage-architecture §2). The size cap and presign
 * TTL bound direct-to-storage uploads; finalize enforces the cap authoritatively from the
 * stored object's size.
 *
 * @param maxBytes   maximum accepted evidence object size
 * @param presignTtl lifetime of a presigned upload URL
 */
@ConfigurationProperties(prefix = "spexcrafters.evidence")
public record EvidenceProperties(Long maxBytes, Duration presignTtl) {

    public EvidenceProperties {
        if (maxBytes == null || maxBytes <= 0) {
            maxBytes = 15L * 1024 * 1024; // 15 MiB
        }
        if (presignTtl == null) {
            presignTtl = Duration.ofMinutes(10);
        }
    }
}
