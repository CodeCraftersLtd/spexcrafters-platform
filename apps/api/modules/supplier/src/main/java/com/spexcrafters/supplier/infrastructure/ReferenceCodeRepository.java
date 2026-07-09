package com.spexcrafters.supplier.infrastructure;

import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Read access to the seeded {@code reference} schema code tables. Table names are chosen from
 * a fixed allowlist (never interpolated from caller input) so the dynamic-table query is
 * injection-safe. Reference data is immutable seed, so the active-code sets are cached.
 */
@Repository
public class ReferenceCodeRepository {

    /** Fixed allowlist of reference tables (kind → fully-qualified table). */
    public enum Kind {
        SUPPLIER_TYPE("reference.supplier_type"),
        SUPPLIER_CAPABILITY("reference.supplier_capability"),
        VERIFICATION_SCOPE("reference.verification_scope"),
        EVIDENCE_TYPE("reference.evidence_type"),
        FACILITY_TYPE("reference.facility_type"),
        SUPPORTED_LOCALE("reference.supported_locale");

        private final String table;

        Kind(String table) {
            this.table = table;
        }
    }

    private final JdbcTemplate jdbc;

    public ReferenceCodeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The active codes of a reference kind (small, static tables). */
    public Set<String> activeCodes(Kind kind) {
        return Set.copyOf(jdbc.queryForList(
                "select code from " + kind.table + " where active = true", String.class));
    }
}
