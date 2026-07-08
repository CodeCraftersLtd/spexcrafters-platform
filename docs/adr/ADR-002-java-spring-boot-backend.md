# ADR-002: Java 25 LTS + Spring Boot 3.5.x backend

## Status
Accepted — 2026-07-08

## Context
SpexCrafters' backend (`apps/api`) carries the platform's entire domain: 20+ bounded contexts, transactional RFQ/quotation workflows, an outbox-driven event system, and a 10+ year expected lifetime. The dependency-longevity policy (master brief §35, technology-decisions.md) mandates LTS-first choices, pinned versions, and deliberate major upgrades. The team needs a language and framework with a deep hiring pool, first-class ORM/domain-modeling support, mature security and OIDC tooling (required for ADR-006 Phase 2), and enough ecosystem inertia to still be well-maintained in a decade.

## Decision
- **Java 25 LTS** as the backend language. Version policy: move only LTS→LTS, and only after the target LTS has 1+ year of maturity.
- **Spring Boot 3.5.x** (with Spring Framework, Spring Security, Spring Data JPA from its BOM) as the application framework. Latest GA of the current major; minors upgraded quarterly, majors deliberately with a human-authored upgrade note.
- **Hibernate ORM** for persistence and **Flyway** as the only DDL mechanism (`ddl-auto=validate` everywhere, never `update`); both track the Spring Boot BOM.
- **Maven** with wrapper-pinned version and the Enforcer plugin (bans SNAPSHOTs and duplicate versions) building the multi-module layout from ADR-001; module rules verified by **ArchUnit** in CI.
- Framework code stays at the edges: `web` and `infrastructure` layers per module; `domain` packages are framework-thin (plain objects owning invariants), per system-architecture.md §G.4.

## Alternatives considered
- **Kotlin (JVM)** — rejected for v1: adds language-level style churn and a second compiler toolchain without solving a problem we have; JVM interop keeps gradual adoption open later if ever desired.
- **Go** — rejected: weaker ORM and rich-domain-modeling story for a heavily relational, aggregate-centric domain (products with attribute templates, RFQ/quotation state machines); smaller ecosystem for the OIDC-server requirement in ADR-006.
- **.NET (C#/ASP.NET Core)** — rejected: technically comparable, but the team's depth, the mandated stack, and the Spring Authorization Server dependency favor the JVM; no offsetting advantage justifies the switch.
- **Quarkus / Micronaut** — rejected: attractive startup/memory profiles, but materially smaller ecosystems and community runway; SpexCrafters' longevity policy prices ecosystem durability above cold-start speed for a long-running monolith.
- **Jakarta EE (application server)** — rejected: heavier operational model, slower release cadence, weaker integration story for OpenTelemetry/Springdoc/Authorization Server compared to Boot.

## Advantages
- Industry-default stack for modular monoliths: enormous maintenance runway, documentation, and hiring pool.
- First-class support for every adjacent decision: Spring Security + Spring Authorization Server (ADR-006), Springdoc for OpenAPI 3.1 (ADR-005), Spring Data JPA + Flyway against PostgreSQL 17 (ADR-004), Micrometer/OTel observability.
- Java LTS cadence matches the longevity policy: predictable, supported, boring.
- Virtual threads (stable since Java 21) let blocking JPA/JDBC code scale request concurrency without reactive rewrites.

## Disadvantages
- Verbosity relative to Kotlin or modern alternatives; more ceremony per feature (partly offset by records, pattern matching, and MapStruct-generated mappers).
- Spring's dependency-injection and auto-configuration "magic" can obscure runtime behavior from newcomers.
- JVM memory baseline is higher than Go-class runtimes; container sizing must account for it.

## Risks
- **Framework magic obscuring behavior** → explicit configuration over auto-config where behavior matters (security filter chain, transaction boundaries); ArchUnit rules keep framework types out of `domain` packages; architecture tests document intent.
- **Major-version upgrade shocks (Boot 3→4, Java 25→next LTS)** → quarterly minor upgrades keep drift small; Renovate-grouped PRs gated by the full test suite (Testcontainers against real PostgreSQL); dedicated quarterly dependency-maintenance day.
- **Hibernate misuse (N+1, entity leakage)** → per-endpoint query budgets asserted in integration tests; JPA entities never cross the `web` boundary (DTO mapping enforced); jOOQ may be added later for complex read queries and can coexist behind repository interfaces.

## Migration path
The domain layer is deliberately framework-thin, which contains porting cost:
- **To Kotlin:** file-by-file interop on the same JVM and Maven build; no big-bang required. This is the cheapest exit and can start in leaf modules.
- **Off Spring:** `domain` packages have no Spring imports; `api` application services depend on constructor injection only. Replacing Boot means rewriting `web` controllers, `infrastructure` adapters, and configuration — significant but bounded per module, and modules can migrate one at a time behind the stable OpenAPI contract.
- **Off Hibernate:** repositories abstract persistence per module; jOOQ or plain JDBC adapters can replace JPA module-by-module; Flyway-owned schema history is ORM-independent.
