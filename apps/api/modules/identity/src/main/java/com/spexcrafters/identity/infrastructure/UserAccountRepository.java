package com.spexcrafters.identity.infrastructure;

import com.spexcrafters.identity.domain.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    /** Case-insensitive at the database level: the email column is {@code citext}. */
    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);
}
