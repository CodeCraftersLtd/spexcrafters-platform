package com.spexcrafters.identity.infrastructure;

import com.spexcrafters.identity.domain.LoginAttempt;
import com.spexcrafters.identity.domain.LoginOutcome;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    long countByEmailAndOutcomeAndAtAfter(String email, LoginOutcome outcome, Instant after);

    Optional<LoginAttempt> findFirstByEmailAndOutcomeAndAtAfterOrderByAtAsc(
            String email, LoginOutcome outcome, Instant after);
}
