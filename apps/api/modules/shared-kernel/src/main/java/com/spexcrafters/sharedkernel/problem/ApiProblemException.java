package com.spexcrafters.sharedkernel.problem;

import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Base exception for business failures that map directly to an RFC 9457 problem response.
 * Translated to {@code application/problem+json} by {@link GlobalExceptionHandler}.
 */
public class ApiProblemException extends RuntimeException {

    private final HttpStatus status;
    private final URI type;
    private final String title;
    private final transient List<ProblemFieldError> errors;
    private final Long retryAfterSeconds;

    public ApiProblemException(HttpStatus status, URI type, String title, String detail) {
        this(status, type, title, detail, List.of(), null);
    }

    public ApiProblemException(HttpStatus status, URI type, String title, String detail,
            List<ProblemFieldError> errors, Long retryAfterSeconds) {
        super(detail);
        this.status = status;
        this.type = type;
        this.title = title;
        this.errors = List.copyOf(errors);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** 422 with a field-error list, for server-side rule violations beyond bean validation. */
    public static ApiProblemException validation(List<ProblemFieldError> errors) {
        return new ApiProblemException(HttpStatus.UNPROCESSABLE_ENTITY, ProblemTypes.VALIDATION_ERROR,
                "Validation failed", "One or more fields are invalid.", errors, null);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public URI getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return getMessage();
    }

    public List<ProblemFieldError> getErrors() {
        return errors;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
