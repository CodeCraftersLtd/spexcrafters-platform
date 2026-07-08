package com.spexcrafters.identity.domain;

import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import com.spexcrafters.sharedkernel.problem.ProblemTypes;
import org.springframework.http.HttpStatus;

/**
 * 403 with problem type {@code .../problems/email-not-verified}: credentials were valid
 * but the email address has not been confirmed yet (contract: EmailNotVerified response).
 */
public class EmailNotVerifiedException extends ApiProblemException {

    public EmailNotVerifiedException() {
        super(HttpStatus.FORBIDDEN, ProblemTypes.EMAIL_NOT_VERIFIED, "Email not verified",
                "Your email address has not been verified yet. Check your inbox or request a new verification email.");
    }
}
