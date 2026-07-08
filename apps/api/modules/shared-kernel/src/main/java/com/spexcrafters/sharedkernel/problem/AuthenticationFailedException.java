package com.spexcrafters.sharedkernel.problem;

import org.springframework.http.HttpStatus;

/** 401 — missing or invalid credentials/token. Detail must never reveal which part failed. */
public class AuthenticationFailedException extends ApiProblemException {

    public AuthenticationFailedException(String detail) {
        super(HttpStatus.UNAUTHORIZED, ProblemTypes.AUTHENTICATION_FAILED, "Authentication failed", detail);
    }
}
