package com.spexcrafters.audit.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spexcrafters.audit.domain.AuditLogEntry;
import com.spexcrafters.audit.infrastructure.AuditLogEntryRepository;
import com.spexcrafters.sharedkernel.util.UuidV7;
import com.spexcrafters.sharedkernel.web.CorrelationIdFilter;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public API of the audit module: records security-relevant events. Joins the caller's
 * transaction so audit rows commit and roll back with the business change they describe.
 * The correlation id is taken from the request MDC populated by {@link CorrelationIdFilter}.
 *
 * <p>Events may carry a structured {@code detail} payload (TD-9), serialized to the
 * {@code jsonb} column. Callers must never put raw tokens, credentials or PII beyond ids
 * into the detail map.
 */
@Service
public class AuditLogger {

    private final AuditLogEntryRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AuditLogger(AuditLogEntryRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void record(String action, UUID actorUserId, String targetType, String targetId) {
        record(action, actorUserId, targetType, targetId, null);
    }

    /**
     * Records an event with a structured {@code detail} payload persisted as jsonb.
     * A {@code null} or empty map records no detail.
     */
    @Transactional
    public void record(String action, UUID actorUserId, String targetType, String targetId,
            Map<String, ?> detail) {
        repository.save(new AuditLogEntry(
                UuidV7.generate(),
                actorUserId,
                action,
                targetType,
                targetId,
                MDC.get(CorrelationIdFilter.MDC_KEY),
                serialize(detail),
                clock.instant()));
    }

    private String serialize(Map<String, ?> detail) {
        if (detail == null || detail.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            // Programmer error: audit detail maps are simple id/name payloads by contract.
            throw new IllegalArgumentException("Audit detail payload is not serializable to JSON", ex);
        }
    }
}
