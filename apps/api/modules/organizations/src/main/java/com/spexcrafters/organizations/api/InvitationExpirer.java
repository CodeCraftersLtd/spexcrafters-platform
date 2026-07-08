package com.spexcrafters.organizations.api;

import com.spexcrafters.organizations.domain.InvitationStatus;
import com.spexcrafters.organizations.domain.OrganizationInvitation;
import com.spexcrafters.organizations.infrastructure.OrganizationInvitationRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies the lazy {@code PENDING → EXPIRED} transition in a {@code REQUIRES_NEW}
 * transaction: acceptance of a past-expiry token throws 410, which rolls the caller's
 * transaction back — the EXPIRED mark must survive that rollback.
 */
@Component
public class InvitationExpirer {

    private final OrganizationInvitationRepository invitations;

    public InvitationExpirer(OrganizationInvitationRepository invitations) {
        this.invitations = invitations;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markExpired(UUID invitationId) {
        invitations.findById(invitationId)
                .filter(invitation -> invitation.getStatus() == InvitationStatus.PENDING)
                .ifPresent(OrganizationInvitation::markExpired);
    }
}
