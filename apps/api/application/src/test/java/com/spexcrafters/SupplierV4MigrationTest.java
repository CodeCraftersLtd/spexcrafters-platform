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
 * Runs the full Flyway history (including V4) against an empty PostgreSQL 17 database and
 * boots the context, exercising Hibernate {@code ddl-auto=validate} against every Phase-7 JPA
 * mapping. Also proves the stepwise V1→V2→V3→V4 upgrade path. CI only (needs Docker); matched
 * by {@code -Dtest=*MigrationTest*}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class SupplierV4MigrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> EMPTY_POSTGRES = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migratesFromEmptyThroughV4AndPassesHibernateValidation() {
        List<String> applied = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success order by installed_rank",
                String.class);
        assertThat(applied).contains("1", "2", "3", "4");
    }

    @Test
    void createsThePhase7Tables() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                select table_schema || '.' || table_name
                from information_schema.tables
                where table_schema in ('supplier', 'verification', 'platform_access', 'reference')
                order by 1
                """,
                String.class);
        assertThat(tables).contains(
                "platform_access.platform_staff",
                "reference.evidence_type",
                "reference.facility_type",
                "reference.supplier_capability",
                "reference.supplier_type",
                "reference.supported_locale",
                "reference.verification_scope",
                "supplier.review_request",
                "supplier.supplier",
                "supplier.supplier_application",
                "supplier.supplier_capability_declaration",
                "supplier.supplier_facility",
                "supplier.supplier_facility_translation",
                "supplier.supplier_profile",
                "supplier.supplier_profile_translation",
                "supplier.supplier_type_assignment",
                "supplier.verification_evidence",
                "verification.scope_result_evidence",
                "verification.verification_case",
                "verification.verification_scope_result");
    }

    @Test
    void seedsTwentyLocalesAndTheFirstVerificationScopes() {
        Integer locales = jdbcTemplate.queryForObject(
                "select count(*) from reference.supported_locale", Integer.class);
        assertThat(locales).isEqualTo(20);
        List<String> scopes = jdbcTemplate.queryForList(
                "select code from reference.verification_scope order by sort_order", String.class);
        assertThat(scopes).containsExactly("LEGAL_ENTITY", "BUSINESS_REGISTRATION", "MANUFACTURER_STATUS",
                "OPTICAL_INDUSTRY_ACTIVITY", "FACTORY_EXISTENCE");
    }

    @Test
    void enforcesTheOneActiveSupplierPartialUniqueIndex() {
        List<String> indexes = jdbcTemplate.queryForList(
                "select indexname from pg_indexes where schemaname = 'supplier' order by 1", String.class);
        assertThat(indexes).contains("uq_supplier_active_org", "uq_profile_translation", "uq_type_assignment");
    }

    @Test
    void migratesStepwiseThroughV4() throws SQLException {
        String database = "stepwise_v4";
        try (Connection connection = DriverManager.getConnection(
                EMPTY_POSTGRES.getJdbcUrl(), EMPTY_POSTGRES.getUsername(), EMPTY_POSTGRES.getPassword());
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        String url = "jdbc:postgresql://" + EMPTY_POSTGRES.getHost() + ":"
                + EMPTY_POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT) + "/" + database;

        migrateTo(url, "3");
        assertThat(appliedVersions(url)).containsExactly("1", "2", "3");
        assertThat(schemaExists(url, "supplier")).isFalse();

        // Target V4 explicitly: this test verifies the V4 stepwise upgrade, so it must stay
        // pinned to V4 and not break as later migrations (V5+) ship. The full V1->latest chain
        // is covered by FlywayMigrationTest.migratesStepwiseThroughEveryVersion.
        migrateTo(url, "4");
        assertThat(appliedVersions(url)).containsExactly("1", "2", "3", "4");
        assertThat(schemaExists(url, "supplier")).isTrue();
        assertThat(schemaExists(url, "verification")).isTrue();
        assertThat(schemaExists(url, "platform_access")).isTrue();
    }

    private static void migrateTo(String url, String targetVersion) {
        var configuration = Flyway.configure()
                .dataSource(url, EMPTY_POSTGRES.getUsername(), EMPTY_POSTGRES.getPassword())
                .locations("classpath:db/migration");
        if (targetVersion != null) {
            configuration = configuration.target(MigrationVersion.fromVersion(targetVersion));
        }
        configuration.load().migrate();
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
