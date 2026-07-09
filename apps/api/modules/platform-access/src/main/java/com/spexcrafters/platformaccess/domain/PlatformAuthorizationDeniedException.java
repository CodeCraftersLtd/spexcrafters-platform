package com.spexcrafters.platformaccess.domain;

import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import org.springframework.http.HttpStatus;

/**
 * 403 — the caller is not active platform staff (or the staff role lacks the capability).
 * There is no 404-concealment here: platform endpoints are not tenant-scoped resources, so
 * a plain "access denied" reveals nothing an authenticated caller could not already infer.
 */
public class PlatformAuthorizationDeniedException extends ApiProblemException {

    public PlatformAuthorizationDeniedException() {
        super(HttpStatus.FORBIDDEN, PlatformProblemTypes.AUTHORIZATION, "Access denied",
                "You do not have platform-staff permission to perform this action.");
    }
}
