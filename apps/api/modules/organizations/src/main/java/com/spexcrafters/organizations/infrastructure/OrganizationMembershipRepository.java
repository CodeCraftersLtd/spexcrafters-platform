package com.spexcrafters.organizations.infrastructure;

import com.spexcrafters.organizations.domain.MembershipStatus;
import com.spexcrafters.organizations.domain.OrganizationMembership;
import com.spexcrafters.organizations.domain.OrganizationRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {

    Optional<OrganizationMembership> findByOrganizationIdAndUserIdAndStatus(
            UUID organizationId, UUID userId, MembershipStatus status);

    boolean existsByOrganizationIdAndUserIdAndStatus(UUID organizationId, UUID userId, MembershipStatus status);

    Optional<OrganizationMembership> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<OrganizationMembership> findByOrganizationIdAndStatusOrderByJoinedAtAsc(
            UUID organizationId, MembershipStatus status);

    List<OrganizationMembership> findByUserIdAndStatusOrderByJoinedAtAsc(UUID userId, MembershipStatus status);

    /** Read under the organization-row lock when enforcing the last-owner invariant. */
    long countByOrganizationIdAndRoleAndStatus(UUID organizationId, OrganizationRole role, MembershipStatus status);
}
