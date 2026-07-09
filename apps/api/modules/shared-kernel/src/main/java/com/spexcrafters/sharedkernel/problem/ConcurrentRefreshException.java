package com.spexcrafters.sharedkernel.problem;

import org.springframework.http.HttpStatus;

/**
 * 401 — the presented refresh token was already rotated by a concurrent request within the
 * grace window (a benign multi-tab race), not theft. Distinct problem {@code type} from
 * {@link AuthenticationFailedException} so the BFF can keep the session alive and let the
 * losing tab continue on the winner's rotated cookie, rather than signing the user out.
 */
public class ConcurrentRefreshException extends ApiProblemException {

    public ConcurrentRefreshException(String detail) {
        super(HttpStatus.UNAUTHORIZED, ProblemTypes.CONCURRENT_REFRESH, "Concurrent refresh", detail);
    }
}
