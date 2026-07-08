package com.spexcrafters.sharedkernel.problem;

import java.util.List;
import org.springframework.http.HttpStatus;

/** 429 — too many requests; carries the {@code Retry-After} value in seconds. */
public class RateLimitedException extends ApiProblemException {

    public RateLimitedException(String detail, long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, ProblemTypes.RATE_LIMITED, "Too many requests",
                detail, List.of(), retryAfterSeconds);
    }
}
