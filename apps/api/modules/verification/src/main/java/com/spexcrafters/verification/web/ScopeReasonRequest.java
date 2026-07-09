package com.spexcrafters.verification.web;

import jakarta.validation.constraints.Size;

/** Optional reason body for suspend/revoke scope operations. */
public record ScopeReasonRequest(@Size(max = 4000) String reason) {

    String reasonOrNull() {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }
}
