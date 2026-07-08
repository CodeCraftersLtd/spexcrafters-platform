package com.spexcrafters.identity.api;

import com.spexcrafters.identity.infrastructure.UserAccountRepository;
import com.spexcrafters.sharedkernel.problem.AuthenticationFailedException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the authenticated principal (JWT {@code sub}) to a {@link UserSummaryDto}. */
@Service
public class CurrentUserService {

    private final UserAccountRepository users;

    public CurrentUserService(UserAccountRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getById(UUID userId) {
        return users.findById(userId)
                .map(UserSummaries::toDto)
                .orElseThrow(() -> new AuthenticationFailedException("This account no longer exists."));
    }
}
