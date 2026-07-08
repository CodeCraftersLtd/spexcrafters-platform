package com.spexcrafters.sharedkernel.problem;

import org.springframework.http.HttpStatus;

/** 409 — the request conflicts with existing state (e.g. email already registered). */
public class ConflictException extends ApiProblemException {

    public ConflictException(String detail) {
        super(HttpStatus.CONFLICT, ProblemTypes.CONFLICT, "Conflict", detail);
    }
}
