package com.spexcrafters.sharedkernel.problem;

import com.spexcrafters.sharedkernel.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Maps exceptions to RFC 9457 {@code application/problem+json} responses matching the
 * {@code Problem} schema of the OpenAPI contract (type, title, status, detail, instance,
 * correlationId, errors[]).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiProblemException.class)
    public ResponseEntity<ProblemDetail> handleApiProblem(ApiProblemException ex, HttpServletRequest request) {
        ProblemDetail problem = baseProblem(ex.getStatus(), ex.getType(), ex.getTitle(), ex.getDetail(), request);
        if (!ex.getErrors().isEmpty()) {
            problem.setProperty("errors", ex.getErrors());
        }
        HttpHeaders headers = new HttpHeaders();
        if (ex.getRetryAfterSeconds() != null) {
            headers.set(HttpHeaders.RETRY_AFTER, Long.toString(ex.getRetryAfterSeconds()));
        }
        return respond(ex.getStatus(), problem, headers);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<ProblemFieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ProblemFieldError(
                        fieldError.getField(),
                        Objects.requireNonNullElse(fieldError.getCode(), "Invalid"),
                        Objects.requireNonNullElse(fieldError.getDefaultMessage(), "Invalid value")))
                .toList();
        return validationProblem(errors, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest request) {
        List<ProblemFieldError> errors = ex.getConstraintViolations().stream()
                .map(violation -> new ProblemFieldError(
                        lastPathNode(violation.getPropertyPath().toString()),
                        violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                        violation.getMessage()))
                .toList();
        return validationProblem(errors, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        ProblemDetail problem = baseProblem(HttpStatus.UNPROCESSABLE_ENTITY, ProblemTypes.VALIDATION_ERROR,
                "Validation failed", "The request body is missing or malformed.", request);
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, problem, new HttpHeaders());
    }

    /**
     * Malformed path/query values (e.g. a non-UUID {@code organizationId}) are client
     * errors, not 500s. A malformed id cannot reference any resource, so a validation
     * problem reveals nothing an attacker did not already know.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        List<ProblemFieldError> errors = List.of(new ProblemFieldError(
                Objects.requireNonNullElse(ex.getName(), "parameter"), "Invalid", "Invalid value"));
        return validationProblem(errors, request);
    }

    /** Safety net for unique-constraint races that slip past explicit existence checks. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        log.warn("Data integrity violation handled as conflict [correlationId={}]",
                MDC.get(CorrelationIdFilter.MDC_KEY));
        ProblemDetail problem = baseProblem(HttpStatus.CONFLICT, ProblemTypes.CONFLICT, "Conflict",
                "The request conflicts with existing data.", request);
        return respond(HttpStatus.CONFLICT, problem, new HttpHeaders());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex,
            HttpServletRequest request) {
        ProblemDetail problem = baseProblem(HttpStatus.UNAUTHORIZED, ProblemTypes.AUTHENTICATION_FAILED,
                "Authentication failed", "Missing or invalid credentials.", request);
        return respond(HttpStatus.UNAUTHORIZED, problem, new HttpHeaders());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex,
            HttpServletRequest request) {
        ProblemDetail problem = baseProblem(HttpStatus.FORBIDDEN, ProblemTypes.FORBIDDEN,
                "Access denied", "You do not have permission to perform this action.", request);
        return respond(HttpStatus.FORBIDDEN, problem, new HttpHeaders());
    }

    /**
     * Framework-raised request errors (unknown path, wrong method, unsupported media type,
     * missing parameter). Handled explicitly so the {@link Exception} catch-all below does
     * not inflate them to 500s; the status comes from the exception itself.
     */
    @ExceptionHandler({
            NoResourceFoundException.class,
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ProblemDetail> handleFrameworkWebErrors(Exception ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(((ErrorResponse) ex).getStatusCode().value());
        ProblemDetail problem = baseProblem(status, URI.create("about:blank"),
                status.getReasonPhrase(), "The request could not be processed.", request);
        return respond(status, problem, new HttpHeaders());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception [correlationId={}]", MDC.get(CorrelationIdFilter.MDC_KEY), ex);
        ProblemDetail problem = baseProblem(HttpStatus.INTERNAL_SERVER_ERROR, ProblemTypes.INTERNAL_ERROR,
                "Internal server error", "An unexpected error occurred.", request);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, problem, new HttpHeaders());
    }

    private ResponseEntity<ProblemDetail> validationProblem(List<ProblemFieldError> errors,
            HttpServletRequest request) {
        ProblemDetail problem = baseProblem(HttpStatus.UNPROCESSABLE_ENTITY, ProblemTypes.VALIDATION_ERROR,
                "Validation failed", "One or more fields are invalid.", request);
        problem.setProperty("errors", errors);
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, problem, new HttpHeaders());
    }

    private ProblemDetail baseProblem(HttpStatus status, URI type, String title, String detail,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(type);
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setInstance(URI.create(request.getRequestURI()));
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
        return problem;
    }

    private ResponseEntity<ProblemDetail> respond(HttpStatus status, ProblemDetail problem, HttpHeaders headers) {
        return ResponseEntity.status(status)
                .headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static String lastPathNode(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot >= 0 ? propertyPath.substring(lastDot + 1) : propertyPath;
    }
}
