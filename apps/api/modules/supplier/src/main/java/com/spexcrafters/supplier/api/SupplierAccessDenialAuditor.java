package com.spexcrafters.supplier.api;

import com.spexcrafters.audit.api.AuditLogger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records {@code authorization.denied} audit events for supplier-scoped requests in a
 * {@code REQUIRES_NEW} transaction, so the audit row survives the rollback that follows the
 * thrown 403/404. The checked capability and the resolved ids travel in the structured
 * {@code detail} payload — never client-supplied ids that were not resolved.
 */
@Component
public class SupplierAccessDenialAuditor {

    private final AuditLogger auditLogger;

    public SupplierAccessDenialAuditor(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDenied(UUID actorUserId, String targetType, UUID targetId,
            String checkedCapability, UUID organizationId) {
        Map<String, String> detail = new HashMap<>();
        detail.put("capability", checkedCapability);
        if (organizationId != null) {
            detail.put("organizationId", organizationId.toString());
        }
        auditLogger.record("authorization.denied", actorUserId, targetType,
                targetId == null ? null : targetId.toString(), detail);
    }
}
