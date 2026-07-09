/**
 * Public API of the verification bounded context: the {@link
 * com.spexcrafters.verification.api.VerificationService} (scope-based grant/suspend/revoke by
 * platform staff, org-scoped status reads) and its DTOs. Cross-module access is permitted only
 * through this {@code ...api} package (ArchUnit-enforced).
 */
package com.spexcrafters.verification.api;
