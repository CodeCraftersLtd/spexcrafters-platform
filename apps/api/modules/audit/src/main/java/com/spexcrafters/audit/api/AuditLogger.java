package com.spexcrafters.audit.api;

import com.spexcrafters.audit.domain.AuditLogEntry;
import com.spexcrafters.audit.infrastructure.AuditLogEntryRepository;
import com.spexcrafters.sharedkernel.util.UuidV7;
import com.spexcrafters.sharedkernel.web.CorrelationIdFilter;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public API of the audit module: records security-relevant events. Joins the caller's
 * transaction so audit rows commit and roll back with the business change they describe.
 * The correlation id is taken from the request MDC populated by {@link CorrelationIdFilter}.
 */
@Service
public class AuditLogger {

    private final AuditLogEntryRepository repository;
    private final Clock clock;

    public AuditLogger(AuditLogEntryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void record(String action, UUID actorUserId, String targetType, String targetId) {
        repository.save(new AuditLogEntry(
                UuidV7.generate(),
                actorUserId,
                action,
                targetType,
                targetId,
                MDC.get(CorrelationIdFilter.MDC_KEY),
                clock.instant()));
    }
}
