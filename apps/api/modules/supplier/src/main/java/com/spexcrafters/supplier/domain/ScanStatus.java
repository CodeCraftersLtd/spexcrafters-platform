package com.spexcrafters.supplier.domain;

/**
 * Malware scan status of evidence (evidence-storage-architecture §3):
 * {@code PENDING_SCAN → SCANNING → CLEAN | REJECTED | QUARANTINED}. The platform never marks
 * evidence {@code CLEAN} in Phase 7 (deferred scanner). {@code QUARANTINED}/{@code REJECTED}
 * are never downloadable; downloads are fail-closed to explicitly downloadable states only.
 */
public enum ScanStatus {
    PENDING_SCAN,
    SCANNING,
    CLEAN,
    REJECTED,
    QUARANTINED;

    /**
     * Whether evidence in this scan status may be downloaded. Fail-closed: only PENDING_SCAN
     * is downloadable in Phase 7 (backend-streamed, audited, with a "not malware-scanned"
     * banner surfaced by the client); QUARANTINED/REJECTED are never downloadable.
     */
    public boolean isDownloadable() {
        return this == PENDING_SCAN || this == CLEAN;
    }
}
