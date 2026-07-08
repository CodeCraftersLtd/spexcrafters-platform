# SpexCrafters API

Java 25 / Spring Boot 3.5 modular-monolith backend for the SpexCrafters Global B2B Optical Marketplace.

## Prerequisites

- **JDK 25** (Temurin) — enforced by maven-enforcer
- **Maven 3.9+** (run `mvn wrapper:wrapper -Dmaven=3.9.9` once to add the wrapper; not committed because this scaffold was authored offline)
- **Docker** — required by Testcontainers integration tests and the local Compose stack

## Run locally

```bash
docker compose -f ../../infrastructure/docker/compose.yaml up -d   # PostgreSQL, Mailpit, …
mvn spring-boot:run -pl application
```

Flyway migrates the schema on startup (`ddl-auto=validate` — Hibernate never writes DDL). API at `http://localhost:8080/api/v1`, OpenAPI at `/v3/api-docs`, health at `/actuator/health/{liveness,readiness}`, Prometheus at `/actuator/prometheus`. Verification emails land in Mailpit (`http://localhost:8025`).

> The `V1` migration runs `CREATE EXTENSION IF NOT EXISTS citext;`, which needs extension-creation privileges. The Compose and Testcontainers databases connect as a superuser, so this only matters for custom environments.

## Configuration (env vars)

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/spexcrafters` | PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `spexcrafters` / `spexcrafters_dev` | Credentials |
| `SPEXCRAFTERS_JWT_SECRET` | dev-only fallback | HS256 signing secret, ≥32 bytes (startup-enforced) |
| `SPRING_MAIL_HOST` / `SPRING_MAIL_PORT` | `localhost` / `1025` | SMTP (Mailpit in dev) |
| `SPEXCRAFTERS_MAIL_FROM` | `no-reply@spexcrafters.com` | Sender address |
| `SPEXCRAFTERS_WEB_BASE_URL` | `http://localhost:3000` | Verification-link base |
| `SPEXCRAFTERS_CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | CORS allowlist |

## Module map

| Module | Contents |
|---|---|
| `modules/shared-kernel` | Problem+json (RFC 9457) error model + global handler, UUIDv7, audited-entity base, correlation-ID filter |
| `modules/identity` | Registration, email verification, login (Argon2id, brute-force throttle), JWT access + rotating refresh tokens, `/me` |
| `modules/audit` | Append-only audit log API consumed by other modules |
| `modules/organizations` | Bounded-context stub (Sprint 2) |
| `application` | Bootable app: config, `application.yml`, structured JSON logging, Flyway migrations (`db/migration`) |
| `architecture-tests` | ArchUnit suite enforcing module boundaries (see `docs/architecture/system-architecture.md` §G.3) |

Cross-module access is legal **only** via a module's `…api` package — enforced by `ModuleArchitectureTest`.

## Tests

```bash
mvn verify                                        # unit + Testcontainers integration + ArchUnit
mvn -pl modules/identity test                     # identity unit tests only
mvn -pl application test -Dtest=*MigrationTest*   # Flyway-from-empty check (mirrors the CI step)
mvn -pl architecture-tests test                   # ArchUnit module rules only
```

Integration tests (`application/src/test`) run the full register → verify → login → `/me` journey, refresh-token rotation with family-revocation-on-reuse, problem+json shape checks, and Flyway migration from an empty PostgreSQL 17 container. They require Docker.

## Contract

The API must stay compatible with the committed contract at [`packages/api-client/spec/openapi.json`](../../packages/api-client/spec/openapi.json). Springdoc exports the live document at `/v3/api-docs`; the CI `contract` job guards against breaking drift.
