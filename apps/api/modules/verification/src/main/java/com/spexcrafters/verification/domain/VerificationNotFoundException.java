package com.spexcrafters.verification.domain;

import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import java.net.URI;
import org.springframework.http.HttpStatus;

/** 404 — the supplier or its verification case/scope result is absent (or concealed). */
public class VerificationNotFoundException extends ApiProblemException {

    private static final URI NOT_FOUND = URI.create("https://api.spexcrafters.com/problems/not-found");

    public VerificationNotFoundException() {
        super(HttpStatus.NOT_FOUND, NOT_FOUND, "Not found", "The requested resource does not exist.");
    }
}
