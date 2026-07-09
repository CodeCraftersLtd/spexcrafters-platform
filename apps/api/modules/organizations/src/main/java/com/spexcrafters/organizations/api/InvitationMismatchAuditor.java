package com.spexcrafters.organizations.api;

import com.spexcrafters.audit.api.AuditLogger;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records {@code organization.invitation.mismatch} audit events (TD-10) in a
 * {@code REQUIRES_NEW} transaction: the mismatch is always followed by a 403 that rolls
 * the acceptance transaction back, and the audit row must survive that rollback (mirrors
 * {@link AuthorizationDenialAuditor}).
 *
 * <p>Threat value: detects invitation-token probing/forwarding. The detail carries ids
 * only ({@code {invitationId, actorUserId}}) — never email addresses.
 */
@Component
public class InvitationMismatchAuditor {

    private final AuditLogger auditLogger;

    public InvitationMismatchAuditor(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMismatch(UUID actorUserId, UUID invitationId) {
        auditLogger.record("organization.invitation.mismatch", actorUserId,
                "organization_invitation", invitationId.toString(),
                Map.of(
                        "invitationId", invitationId.toString(),
                        "actorUserId", actorUserId.toString()));
    }
}
