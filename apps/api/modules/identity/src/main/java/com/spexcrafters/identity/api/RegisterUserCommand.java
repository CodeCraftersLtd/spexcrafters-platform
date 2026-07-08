package com.spexcrafters.identity.api;

/**
 * Registration input. {@code localeCode} is a wire locale code ({@code en}, {@code zh-Hans},
 * {@code fr}, {@code de}); the web layer validates it and applies the {@code en} default.
 */
public record RegisterUserCommand(
        String email,
        String password,
        String displayName,
        String localeCode) {
}
