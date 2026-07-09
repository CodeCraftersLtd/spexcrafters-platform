package com.spexcrafters.supplier.domain;

import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import org.springframework.http.HttpStatus;

/**
 * 409 — the organization already has an active supplier identity (invariant: at most one
 * supplier per organization whose application is not WITHDRAWN/REJECTED).
 */
public class OneActiveSupplierException extends ApiProblemException {

    public OneActiveSupplierException() {
        super(HttpStatus.CONFLICT, SupplierProblemTypes.ONE_ACTIVE_SUPPLIER,
                "Organization already has a supplier",
                "This organization already has an active supplier application.");
    }
}
