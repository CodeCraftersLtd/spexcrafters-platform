package com.spexcrafters.identity.infrastructure.security;

import com.spexcrafters.sharedkernel.problem.ProblemJsonWriter;
import com.spexcrafters.sharedkernel.problem.ProblemTypes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Renders 401s raised inside the security filter chain (missing/expired/invalid bearer
 * token) as RFC 9457 problem+json, consistent with the controller-advice error model.
 */
@Component
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemJsonWriter problemJsonWriter;

    public ProblemAuthenticationEntryPoint(ProblemJsonWriter problemJsonWriter) {
        this.problemJsonWriter = problemJsonWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        problemJsonWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                ProblemTypes.AUTHENTICATION_FAILED, "Authentication failed",
                "Missing or invalid access token.");
    }
}
