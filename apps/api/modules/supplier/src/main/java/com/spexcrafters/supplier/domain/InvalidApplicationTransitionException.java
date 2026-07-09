package com.spexcrafters.supplier.domain;

import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import org.springframework.http.HttpStatus;

/** 409 — an application lifecycle transition was requested that the current state forbids. */
public class InvalidApplicationTransitionException extends ApiProblemException {

    public InvalidApplicationTransitionException(ApplicationStatus from, ApplicationStatus to) {
        super(HttpStatus.CONFLICT, SupplierProblemTypes.INVALID_APPLICATION_STATE,
                "Invalid application state transition",
                "The application cannot move from " + from + " to " + to + ".");
    }
}
