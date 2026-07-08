package com.spexcrafters.audit.infrastructure;

import com.spexcrafters.audit.domain.AuditLogEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogEntryRepository extends JpaRepository<AuditLogEntry, UUID> {
}
