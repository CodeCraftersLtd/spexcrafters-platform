package com.spexcrafters.organizations.api;

import com.spexcrafters.audit.api.AuditLogger;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records {@code authorization.denied} audit events in a {@code REQUIRES_NEW} transaction,
 * because the denial is always followed by an exception that rolls the caller's
 * transaction back — the audit row must survive that rollback.
 *
 * <p>{@code target_id} is the plain organization id (so the {@code (target_type,
 * target_id)} index stays a clean lookup key); the checked capability travels in the
 * structured {@code detail} payload ({@code {capability, organizationId}} — TD-9).
 */
@Component
public class AuthorizationDenialAuditor {

    private final AuditLogger auditLogger;

    public AuthorizationDenialAuditor(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDenied(UUID actorUserId, UUID organizationId, String checkedCapability) {
        auditLogger.record("authorization.denied", actorUserId, "organization",
                organizationId.toString(),
                Map.of(
                        "capability", checkedCapability,
                        "organizationId", organizationId.toString()));
    }
}
