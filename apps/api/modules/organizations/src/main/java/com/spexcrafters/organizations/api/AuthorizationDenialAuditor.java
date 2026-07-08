package com.spexcrafters.organizations.api;

import com.spexcrafters.audit.api.AuditLogger;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records {@code authorization.denied} audit events in a {@code REQUIRES_NEW} transaction,
 * because the denial is always followed by an exception that rolls the caller's
 * transaction back — the audit row must survive that rollback.
 *
 * <p>The checked capability is encoded into {@code target_id} alongside the organization id
 * ({@code "<orgId> capability=<wire-name>"}) since the audit log's target column pair is
 * the only structured payload available; {@code target_type} stays {@code organization}
 * so the (target_type, target_id) index remains useful.
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
                organizationId + " capability=" + checkedCapability);
    }
}
