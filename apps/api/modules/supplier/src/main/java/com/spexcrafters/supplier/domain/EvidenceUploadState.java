package com.spexcrafters.supplier.domain;

/**
 * Staged-upload state of an evidence row (evidence-storage-architecture §2). An
 * {@code INITIATED} row has a presigned PUT outstanding but no server-validated bytes;
 * {@code FINALIZED} means the server verified presence, size, sha256 and magic bytes. Only
 * FINALIZED evidence is listable/downloadable/linkable; unfinalized rows past their TTL are
 * reaped.
 */
public enum EvidenceUploadState {
    INITIATED,
    FINALIZED
}
