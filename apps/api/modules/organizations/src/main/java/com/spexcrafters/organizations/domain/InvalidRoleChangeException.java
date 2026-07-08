package com.spexcrafters.organizations.domain;

import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import org.springframework.http.HttpStatus;

/**
 * 409 — a role change forbidden by policy. The capability model forbids self role-change
 * outright (prevents accidental lockout and self-escalation); we surface it as a 409 with
 * the dedicated problem type {@code invalid-role-change} because the request conflicts
 * with the resource's state (the caller's own membership), not with its syntax.
 */
public class InvalidRoleChangeException extends ApiProblemException {

    public InvalidRoleChangeException(String detail) {
        super(HttpStatus.CONFLICT, OrganizationProblemTypes.INVALID_ROLE_CHANGE, "Invalid role change", detail);
    }
}
