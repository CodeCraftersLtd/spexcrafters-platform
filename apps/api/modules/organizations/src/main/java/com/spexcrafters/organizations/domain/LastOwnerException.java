package com.spexcrafters.organizations.domain;

import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import org.springframework.http.HttpStatus;

/**
 * 409 — the mutation would leave an ACTIVE organization without any ACTIVE OWNER
 * (last-owner invariant, organizations-capability-model.md §3).
 */
public class LastOwnerException extends ApiProblemException {

    public LastOwnerException() {
        super(HttpStatus.CONFLICT, OrganizationProblemTypes.LAST_OWNER, "Last owner",
                "An organization must keep at least one active owner. Promote another member to OWNER first.");
    }
}
