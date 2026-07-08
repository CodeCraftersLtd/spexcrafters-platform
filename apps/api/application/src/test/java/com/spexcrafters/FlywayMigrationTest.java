package com.spexcrafters;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
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
 * Additionally proves the stepwise upgrade path V1 → V2 on a separate database, mirroring
 * what a deployed environment that already ran V1 will execute.
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

        assertThat(applied).contains("1", "2");
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

    @Test
    void createsTheOrganizationsTables() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                select table_schema || '.' || table_name
                from information_schema.tables
                where table_schema = 'organizations'
                order by 1
                """,
                String.class);

        assertThat(tables).containsExactly(
                "organizations.invitation",
                "organizations.membership",
                "organizations.organization");

        // The lifecycle-critical partial unique indexes exist.
        List<String> indexes = jdbcTemplate.queryForList(
                "select indexname from pg_indexes where schemaname = 'organizations' order by 1",
                String.class);
        assertThat(indexes).contains(
                "uq_membership_active_org_user",
                "uq_invitation_pending_org_email");
    }

    /**
     * Applies V1 only (Flyway {@code target=1}), then migrates the rest — the exact path a
     * database that was baselined on the identity slice takes when V2 ships.
     */
    @Test
    void migratesStepwiseFromV1ToV2() throws SQLException {
        String database = "stepwise_upgrade";
        try (Connection connection = DriverManager.getConnection(
                EMPTY_POSTGRES.getJdbcUrl(), EMPTY_POSTGRES.getUsername(), EMPTY_POSTGRES.getPassword());
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        String url = "jdbc:postgresql://" + EMPTY_POSTGRES.getHost() + ":"
                + EMPTY_POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT) + "/" + database;

        Flyway upToV1 = Flyway.configure()
                .dataSource(url, EMPTY_POSTGRES.getUsername(), EMPTY_POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("1"))
                .load();
        upToV1.migrate();
        assertThat(appliedVersions(url)).containsExactly("1");
        assertThat(schemaExists(url, "organizations")).isFalse();

        Flyway toLatest = Flyway.configure()
                .dataSource(url, EMPTY_POSTGRES.getUsername(), EMPTY_POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();
        toLatest.migrate();
        assertThat(appliedVersions(url)).containsExactly("1", "2");
        assertThat(schemaExists(url, "organizations")).isTrue();
    }

    private static List<String> appliedVersions(String url) throws SQLException {
        List<String> versions = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(
                url, EMPTY_POSTGRES.getUsername(), EMPTY_POSTGRES.getPassword());
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "select version from flyway_schema_history where success order by installed_rank")) {
            while (resultSet.next()) {
                versions.add(resultSet.getString(1));
            }
        }
        return versions;
    }

    private static boolean schemaExists(String url, String schema) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                url, EMPTY_POSTGRES.getUsername(), EMPTY_POSTGRES.getPassword());
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "select 1 from information_schema.schemata where schema_name = '" + schema + "'")) {
            return resultSet.next();
        }
    }
}
