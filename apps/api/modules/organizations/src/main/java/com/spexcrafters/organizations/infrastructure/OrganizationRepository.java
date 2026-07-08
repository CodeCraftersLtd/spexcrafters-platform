package com.spexcrafters.organizations.infrastructure;

import com.spexcrafters.organizations.domain.Organization;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    /**
     * Acquires a {@code PESSIMISTIC_WRITE} ({@code SELECT ... FOR UPDATE}) lock on the
     * organization row. Every owner-affecting membership mutation (remove-owner,
     * demote-owner, owner-leave) takes this lock first, serializing concurrent mutations
     * so the last-owner invariant check cannot race.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Organization o where o.id = :id")
    Optional<Organization> lockForOwnerMutation(@Param("id") UUID id);
}
