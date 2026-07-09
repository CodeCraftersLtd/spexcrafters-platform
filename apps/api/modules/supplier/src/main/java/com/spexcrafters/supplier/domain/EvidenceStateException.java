package com.spexcrafters.supplier.domain;

import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import org.springframework.http.HttpStatus;

/**
 * 409 — an evidence operation is not permitted in the item's current state (e.g. deleting
 * retention-protected evidence, or downloading quarantined evidence).
 */
public class EvidenceStateException extends ApiProblemException {

    public EvidenceStateException(String detail) {
        super(HttpStatus.CONFLICT, SupplierProblemTypes.EVIDENCE_STATE, "Evidence unavailable", detail);
    }
}
