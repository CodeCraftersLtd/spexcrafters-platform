package com.spexcrafters.identity.api;

/** Login input; {@code ipAddress} is recorded on the login attempt for abuse forensics. */
public record LoginCommand(
        String email,
        String password,
        String ipAddress) {
}
