package com.spexcrafters.identity.api;

import com.spexcrafters.identity.domain.UserAccount;

/** Domain-to-DTO mapping shared by the identity application services. */
final class UserSummaries {

    private UserSummaries() {
    }

    static UserSummaryDto toDto(UserAccount user) {
        return new UserSummaryDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getLocale(),
                user.isEmailVerified(),
                user.getCreatedAt());
    }
}
