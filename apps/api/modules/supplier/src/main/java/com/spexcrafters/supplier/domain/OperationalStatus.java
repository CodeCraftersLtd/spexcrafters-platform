package com.spexcrafters.supplier.domain;

/**
 * Operational status of a {@link Supplier} (supplier-domain-model §3). Distinct from the
 * application lifecycle: an application is APPROVED (terminal) and that approval flips the
 * supplier to {@code ACTIVE} in the same transaction. Suspension acts on the supplier, never
 * the application.
 */
public enum OperationalStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    DEACTIVATED
}
