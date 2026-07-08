package com.spexcrafters.identity.infrastructure.security;

import com.spexcrafters.sharedkernel.problem.ProblemJsonWriter;
import com.spexcrafters.sharedkernel.problem.ProblemTypes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Renders 403s raised inside the security filter chain as RFC 9457 problem+json. */
@Component
public class ProblemAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemJsonWriter problemJsonWriter;

    public ProblemAccessDeniedHandler(ProblemJsonWriter problemJsonWriter) {
        this.problemJsonWriter = problemJsonWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        problemJsonWriter.write(request, response, HttpStatus.FORBIDDEN,
                ProblemTypes.FORBIDDEN, "Access denied",
                "You do not have permission to perform this action.");
    }
}
