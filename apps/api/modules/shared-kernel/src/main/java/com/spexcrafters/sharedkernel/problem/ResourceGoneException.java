package com.spexcrafters.sharedkernel.problem;

import org.springframework.http.HttpStatus;

/** 410 — a single-use or time-limited resource (e.g. verification token) is no longer usable. */
public class ResourceGoneException extends ApiProblemException {

    public ResourceGoneException(String detail) {
        super(HttpStatus.GONE, ProblemTypes.TOKEN_GONE, "No longer available", detail);
    }
}
