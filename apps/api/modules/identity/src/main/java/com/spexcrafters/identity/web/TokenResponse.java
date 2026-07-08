package com.spexcrafters.identity.web;

import com.spexcrafters.identity.api.IssuedTokens;
import com.spexcrafters.identity.api.UserSummaryDto;

/** The {@code TokenResponse} schema of the OpenAPI contract. */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        UserSummaryDto user) {

    static TokenResponse from(IssuedTokens issued) {
        return new TokenResponse(
                issued.accessToken(),
                "Bearer",
                issued.expiresInSeconds(),
                issued.refreshToken(),
                issued.user());
    }
}
