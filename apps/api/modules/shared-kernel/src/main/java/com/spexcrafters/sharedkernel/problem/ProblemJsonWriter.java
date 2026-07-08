package com.spexcrafters.sharedkernel.problem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spexcrafters.sharedkernel.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Writes RFC 9457 problem responses directly to the servlet response. Used where the
 * {@code @RestControllerAdvice} cannot run — i.e. inside the Spring Security filter chain
 * (authentication entry point, access denied handler).
 */
@Component
public class ProblemJsonWriter {

    private final ObjectMapper objectMapper;

    public ProblemJsonWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
            URI type, String title, String detail) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type.toString());
        body.put("title", title);
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("instance", request.getRequestURI());
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            body.put("correlationId", correlationId);
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
