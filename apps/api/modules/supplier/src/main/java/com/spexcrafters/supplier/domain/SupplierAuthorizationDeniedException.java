package com.spexcrafters.supplier.domain;

import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import org.springframework.http.HttpStatus;

/**
 * 403 — the caller is a member of the owning organization (so its existence is not concealed)
 * but the org role lacks the required supplier capability.
 */
public class SupplierAuthorizationDeniedException extends ApiProblemException {

    public SupplierAuthorizationDeniedException() {
        super(HttpStatus.FORBIDDEN, SupplierProblemTypes.AUTHORIZATION, "Access denied",
                "You do not have permission to perform this action.");
    }
}
