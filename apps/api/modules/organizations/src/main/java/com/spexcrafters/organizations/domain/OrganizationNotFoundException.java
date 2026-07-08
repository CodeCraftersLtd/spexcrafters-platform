package com.spexcrafters.organizations.domain;

import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import org.springframework.http.HttpStatus;

/**
 * 404 — the organization-scoped resource is absent, or its existence is concealed from a
 * non-member by tenancy policy (organizations-capability-model.md §5). The detail is
 * deliberately uniform so responses cannot be used to probe organization existence.
 */
public class OrganizationNotFoundException extends ApiProblemException {

    public OrganizationNotFoundException() {
        this("The requested resource was not found.");
    }

    public OrganizationNotFoundException(String detail) {
        super(HttpStatus.NOT_FOUND, OrganizationProblemTypes.NOT_FOUND, "Not found", detail);
    }
}
