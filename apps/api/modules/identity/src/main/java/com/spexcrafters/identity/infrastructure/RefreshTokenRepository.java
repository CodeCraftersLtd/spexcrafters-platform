package com.spexcrafters.identity.infrastructure;

import com.spexcrafters.identity.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Locking variant ({@code SELECT ... FOR UPDATE}) used by refresh rotation: concurrent
     * refreshes of the same token serialize on the row, so exactly one rotates and the
     * others observe the committed rotation (and fall into the grace-window path) instead
     * of racing into an optimistic-lock failure.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    /**
     * Revokes every not-yet-revoked token of a family (logout, or theft detection on
     * refresh-token reuse). Bulk JPQL update on purpose: one statement regardless of
     * family size, no entity loading.
     */
    @Modifying(clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

    /**
     * The family's creation instant — the {@code created_at} of its oldest token, i.e. the
     * login that started the session. Drives the absolute-session-lifetime check; served
     * by {@code ix_refresh_token_family_id}.
     */
    @Query("select min(t.createdAt) from RefreshToken t where t.familyId = :familyId")
    Optional<Instant> findFamilyCreatedAt(@Param("familyId") UUID familyId);
}
