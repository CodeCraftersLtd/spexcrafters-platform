package com.spexcrafters.supplier.domain;

/**
 * Retention guard on evidence (evidence-storage-architecture §5). {@code RETAINED} evidence
 * is tied to a decided verification and cannot be freely deleted by the supplier; only
 * unreferenced {@code NONE} evidence is deletable.
 */
public enum RetentionStatus {
    NONE,
    RETAINED
}
