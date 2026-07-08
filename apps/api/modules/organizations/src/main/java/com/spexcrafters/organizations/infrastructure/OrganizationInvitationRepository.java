package com.spexcrafters.organizations.infrastructure;

import com.spexcrafters.organizations.domain.InvitationStatus;
import com.spexcrafters.organizations.domain.OrganizationInvitation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationInvitationRepository extends JpaRepository<OrganizationInvitation, UUID> {

    Optional<OrganizationInvitation> findByTokenHash(String tokenHash);

    Optional<OrganizationInvitation> findByIdAndOrganizationId(UUID id, UUID organizationId);

    /** Case-insensitive at the database level: the email column is {@code citext}. */
    boolean existsByOrganizationIdAndEmailAndStatus(UUID organizationId, String email, InvitationStatus status);

    List<OrganizationInvitation> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
