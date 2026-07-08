package com.spexcrafters.organizations.domain;

import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import org.springframework.http.HttpStatus;

/** 409 — the invited/accepting user already holds an ACTIVE membership in the organization. */
public class DuplicateMembershipException extends ApiProblemException {

    public DuplicateMembershipException() {
        super(HttpStatus.CONFLICT, OrganizationProblemTypes.DUPLICATE_MEMBERSHIP, "Duplicate membership",
                "This user is already an active member of the organization.");
    }
}
