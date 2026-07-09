package com.spexcrafters.platformaccess.api;

import com.spexcrafters.audit.api.AuditLogger;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records {@code authorization.denied} audit events for platform moderation in a
 * {@code REQUIRES_NEW} transaction, so the audit row survives the rollback that follows the
 * thrown 403. The checked capability travels in the structured {@code detail} payload.
 */
@Component
public class PlatformAccessDenialAuditor {

    private final AuditLogger auditLogger;

    public PlatformAccessDenialAuditor(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDenied(UUID actorUserId, String checkedCapability) {
        auditLogger.record("authorization.denied", actorUserId, "platform",
                actorUserId == null ? null : actorUserId.toString(),
                Map.of("capability", checkedCapability, "scope", "platform"));
    }
}
