package com.spexcrafters.supplier.domain;

/**
 * Translation lifecycle (ADR-020): {@code MISSING → DRAFT → MACHINE_TRANSLATED →
 * HUMAN_REVIEWED → APPROVED} (+ {@code REJECTED}). Machine-translated content is never shown
 * as human-verified. A translation whose {@code sourceVersion} lags the entity's current
 * {@code sourceVersion} is stale regardless of status.
 */
public enum TranslationStatus {
    MISSING,
    DRAFT,
    MACHINE_TRANSLATED,
    HUMAN_REVIEWED,
    APPROVED,
    REJECTED
}
