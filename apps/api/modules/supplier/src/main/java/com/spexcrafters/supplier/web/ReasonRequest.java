package com.spexcrafters.supplier.web;

import jakarta.validation.constraints.Size;

/** Optional free-text reason body shared by reject and suspend operations. */
public record ReasonRequest(@Size(max = 4000) String reason) {

    String reasonOrNull() {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }
}
