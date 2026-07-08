package com.spexcrafters.organizations.domain;

import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import org.springframework.http.HttpStatus;

/**
 * 403 — the invitation was issued for a different email address than the authenticated
 * account's. The detail deliberately reveals no organization details.
 */
public class InvitationIdentityMismatchException extends ApiProblemException {

    public InvitationIdentityMismatchException() {
        super(HttpStatus.FORBIDDEN, OrganizationProblemTypes.INVITATION_IDENTITY_MISMATCH,
                "Invitation identity mismatch",
                "This invitation was issued for a different email address. Sign in with the invited account.");
    }
}
