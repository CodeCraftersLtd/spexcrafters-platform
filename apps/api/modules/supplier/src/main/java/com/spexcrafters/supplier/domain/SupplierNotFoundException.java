package com.spexcrafters.supplier.domain;

import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import org.springframework.http.HttpStatus;

/**
 * 404 — the supplier/application/evidence is absent, or concealed from a caller who is not a
 * member of its owning organization (mirrors organization tenancy concealment, defeating
 * cross-tenant probing and IDOR).
 */
public class SupplierNotFoundException extends ApiProblemException {

    public SupplierNotFoundException() {
        super(HttpStatus.NOT_FOUND, SupplierProblemTypes.NOT_FOUND, "Not found",
                "The requested resource does not exist.");
    }
}
