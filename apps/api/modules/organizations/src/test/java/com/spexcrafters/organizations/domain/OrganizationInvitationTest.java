package com.spexcrafters.organizations.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.spexcrafters.sharedkernel.util.UuidV7;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrganizationInvitationTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Instant EXPIRY = NOW.plusSeconds(7 * 24 * 3600);

    private OrganizationInvitation invitation() {
        return new OrganizationInvitation(
                UuidV7.generate(), UUID.randomUUID(), "invitee@example.com",
                OrganizationRole.MEMBER, InvitationToken.sha256Hex("raw"), UUID.randomUUID(), EXPIRY);
    }

    @Test
    void newInvitationIsPending() {
        OrganizationInvitation invitation = invitation();
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(invitation.getAcceptedAt()).isNull();
        assertThat(invitation.getAcceptedBy()).isNull();
    }

    @Test
    void ownerRoleCannotBeInvited() {
        assertThatIllegalArgumentException().isThrownBy(() -> new OrganizationInvitation(
                UuidV7.generate(), UUID.randomUUID(), "invitee@example.com",
                OrganizationRole.OWNER, InvitationToken.sha256Hex("raw"), UUID.randomUUID(), EXPIRY));
    }

    @Test
    void expiryBoundaryIsInclusive() {
        OrganizationInvitation invitation = invitation();
        assertThat(invitation.isExpired(EXPIRY.minusMillis(1))).isFalse();
        assertThat(invitation.isExpired(EXPIRY)).isTrue();
        assertThat(invitation.isExpired(EXPIRY.plusMillis(1))).isTrue();
    }

    @Test
    void acceptIsSingleUse() {
        OrganizationInvitation invitation = invitation();
        UUID acceptor = UUID.randomUUID();

        invitation.markAccepted(NOW, acceptor);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(invitation.getAcceptedAt()).isEqualTo(NOW);
        assertThat(invitation.getAcceptedBy()).isEqualTo(acceptor);

        assertThatIllegalStateException().isThrownBy(() -> invitation.markAccepted(NOW, acceptor));
        assertThatIllegalStateException().isThrownBy(invitation::markRevoked);
        assertThatIllegalStateException().isThrownBy(invitation::markExpired);
    }

    @Test
    void expiredTokenCannotBeAccepted() {
        OrganizationInvitation invitation = invitation();
        assertThatIllegalStateException()
                .isThrownBy(() -> invitation.markAccepted(EXPIRY.plusSeconds(1), UUID.randomUUID()));
    }

    @Test
    void revokeAndExpireOnlyApplyToPending() {
        OrganizationInvitation revoked = invitation();
        revoked.markRevoked();
        assertThat(revoked.getStatus()).isEqualTo(InvitationStatus.REVOKED);
        assertThatIllegalStateException().isThrownBy(revoked::markRevoked);
        assertThatIllegalStateException().isThrownBy(revoked::markExpired);
        assertThatIllegalStateException().isThrownBy(() -> revoked.markAccepted(NOW, UUID.randomUUID()));

        OrganizationInvitation expired = invitation();
        expired.markExpired();
        assertThat(expired.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
        assertThatIllegalStateException().isThrownBy(expired::markRevoked);
    }
}
