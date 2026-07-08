package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs the Flyway history against an empty PostgreSQL 17 database (its own container, not
 * the shared one) and boots the full context, which also exercises Hibernate's
 * {@code ddl-auto=validate} check of every JPA mapping against the migrated schema.
 * Matched by the CI step {@code -Dtest=*MigrationTest*}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class FlywayMigrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> EMPTY_POSTGRES = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migratesFromEmptySchemaAndPassesHibernateValidation() {
        List<String> applied = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success order by installed_rank",
                String.class);

        assertThat(applied).contains("1");
    }

    @Test
    void createsTheIdentityAndAuditTables() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                select table_schema || '.' || table_name
                from information_schema.tables
                where table_schema in ('identity', 'audit')
                order by 1
                """,
                String.class);

        assertThat(tables).contains(
                "audit.audit_log",
                "identity.email_verification_token",
                "identity.login_attempt",
                "identity.refresh_token",
                "identity.user_account");
    }
}
