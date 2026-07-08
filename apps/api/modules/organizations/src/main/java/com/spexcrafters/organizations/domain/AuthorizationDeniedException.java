package com.spexcrafters.organizations.domain;

import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import org.springframework.http.HttpStatus;

/**
 * 403 — the caller is a member of the organization (so its existence is not concealed)
 * but lacks the required capability or violates a rank rule.
 */
public class AuthorizationDeniedException extends ApiProblemException {

    public AuthorizationDeniedException() {
        this("You do not have permission to perform this action.");
    }

    public AuthorizationDeniedException(String detail) {
        super(HttpStatus.FORBIDDEN, OrganizationProblemTypes.AUTHORIZATION, "Access denied", detail);
    }
}
