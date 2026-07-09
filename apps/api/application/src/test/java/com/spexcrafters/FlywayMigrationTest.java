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
 * Additionally proves the stepwise upgrade path V1 → V2 → V3 → V4 on a separate database,
 * mirroring what a deployed environment that already ran earlier versions will execute.
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

        assertThat(applied).contains("1", "2", "3");
    }

    @Test
    void addsTheJsonbAuditDetailColumn() {
        // V3 (TD-9): audit.audit_log.detail is nullable jsonb.
        List<String> detailColumn = jdbcTemplate.queryForList(
                """
                select data_type || ':' || is_nullable
                from information_schema.columns
                where table_schema = 'audit' and table_name = 'audit_log' and column_name = 'detail'
                """,
                String.class);

        assertThat(detailColumn).containsExactly("jsonb:YES");
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
     * Applies each version in turn (Flyway {@code target}), asserting the intermediate
     * states — the exact path a deployed database that was baselined on an earlier slice
     * takes when the next migration ships.
     */
    @Test
    void migratesStepwiseThroughEveryVersion() throws SQLException {
        String database = "stepwise_upgrade";
        try (Connection connection = DriverManager.getConnection(
                EMPTY_POSTGRES.getJdbcUrl(), EMPTY_POSTGRES.getUsername(), EMPTY_POSTGRES.getPassword());
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        String url = "jdbc:postgresql://" + EMPTY_POSTGRES.getHost() + ":"
                + EMPTY_POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT) + "/" + database;

        migrateTo(url, "1");
        assertThat(appliedVersions(url)).containsExactly("1");
        assertThat(schemaExists(url, "organizations")).isFalse();

        migrateTo(url, "2");
        assertThat(appliedVersions(url)).containsExactly("1", "2");
        assertThat(schemaExists(url, "organizations")).isTrue();
        assertThat(auditDetailColumnExists(url)).isFalse();

        migrateTo(url, "3");
        assertThat(appliedVersions(url)).containsExactly("1", "2", "3");
        assertThat(auditDetailColumnExists(url)).isTrue();
        assertThat(schemaExists(url, "supplier")).isFalse();

        migrateTo(url, "4");
        assertThat(appliedVersions(url)).containsExactly("1", "2", "3", "4");
        assertThat(schemaExists(url, "supplier")).isTrue();
        assertThat(schemaExists(url, "taxonomy")).isFalse();

        migrateTo(url, null);
        assertThat(appliedVersions(url)).containsExactly("1", "2", "3", "4", "5");
        assertThat(schemaExists(url, "taxonomy")).isTrue();
        assertThat(schemaExists(url, "supplier")).isTrue();
    }

    /** Migrates {@code url} up to {@code targetVersion} ({@code null} = latest). */
    private static void migrateTo(String url, String targetVersion) {
        var configuration = Flyway.configure()
                .dataSource(url, EMPTY_POSTGRES.getUsername(), EMPTY_POSTGRES.getPassword())
                .locations("classpath:db/migration");
        if (targetVersion != null) {
            configuration = configuration.target(MigrationVersion.fromVersion(targetVersion));
        }
        configuration.load().migrate();
    }

    private static boolean auditDetailColumnExists(String url) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                url, EMPTY_POSTGRES.getUsername(), EMPTY_POSTGRES.getPassword());
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        """
                        select 1 from information_schema.columns
                        where table_schema = 'audit' and table_name = 'audit_log'
                          and column_name = 'detail' and data_type = 'jsonb'
                        """)) {
            return resultSet.next();
        }
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
