package com.spexcrafters.supplier.domain;

import java.util.Set;

/**
 * Supplier application lifecycle (supplier-domain-model §4):
 * {@code DRAFT → SUBMITTED → UNDER_REVIEW → (CHANGES_REQUESTED → RESUBMITTED → UNDER_REVIEW)*
 * → APPROVED | REJECTED}; {@code WITHDRAWN} from any pre-decision state. Allowed transitions
 * are declared here and enforced by {@link SupplierApplication}; an invalid transition is a
 * 409 conflict.
 */
public enum ApplicationStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    CHANGES_REQUESTED,
    RESUBMITTED,
    APPROVED,
    REJECTED,
    WITHDRAWN;

    /** The states an application may legally move to from this one. */
    public Set<ApplicationStatus> allowedTransitions() {
        return switch (this) {
            case DRAFT -> Set.of(SUBMITTED, WITHDRAWN);
            case SUBMITTED -> Set.of(UNDER_REVIEW, WITHDRAWN);
            case UNDER_REVIEW -> Set.of(CHANGES_REQUESTED, APPROVED, REJECTED, WITHDRAWN);
            case CHANGES_REQUESTED -> Set.of(RESUBMITTED, WITHDRAWN);
            case RESUBMITTED -> Set.of(UNDER_REVIEW, WITHDRAWN);
            case APPROVED, REJECTED, WITHDRAWN -> Set.of();
        };
    }

    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }

    /** Whether the supplier org may still edit the draft/change-response content. */
    public boolean isEditableBySupplier() {
        return this == DRAFT || this == CHANGES_REQUESTED;
    }
}
