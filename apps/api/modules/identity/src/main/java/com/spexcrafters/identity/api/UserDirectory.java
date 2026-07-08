package com.spexcrafters.identity.api;

import com.spexcrafters.identity.infrastructure.UserAccountRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only user lookups for other modules (e.g. organizations renders member lists with
 * display names and matches invitations to accounts). Part of the identity module's public
 * {@code api} surface so cross-module access never touches identity internals or tables.
 */
@Service
public class UserDirectory {

    private final UserAccountRepository users;

    public UserDirectory(UserAccountRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> findSummariesByIds(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return users.findAllById(userIds).stream()
                .map(UserSummaries::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<UserSummaryDto> findById(UUID userId) {
        return users.findById(userId).map(UserSummaries::toDto);
    }

    /** Case-insensitive at the database level: the email column is {@code citext}. */
    @Transactional(readOnly = true)
    public Optional<UserSummaryDto> findByEmail(String email) {
        return users.findByEmail(email).map(UserSummaries::toDto);
    }
}
