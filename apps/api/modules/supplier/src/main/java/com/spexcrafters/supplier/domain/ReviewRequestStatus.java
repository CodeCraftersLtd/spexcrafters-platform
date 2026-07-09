package com.spexcrafters.supplier.domain;

/** Lifecycle of a change request raised by a reviewer (supplier-domain-model §3). */
public enum ReviewRequestStatus {
    OPEN,
    RESPONDED,
    RESOLVED
}
