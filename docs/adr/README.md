# Architecture Decision Records — SpexCrafters

**Project:** SpexCrafters — Global B2B Optical Marketplace and Sourcing Platform
**Source of truth for decisions:** [Technology Decision Matrix](../architecture/technology-decisions.md)

This directory records the significant architectural decisions for SpexCrafters. Each ADR is immutable once Accepted; a decision is changed by writing a new ADR that supersedes it, never by editing history.

## Index

| ADR | Title | Status | Date | Scope |
|---|---|---|---|---|
| [001](ADR-001-modular-monolith.md) | Modular monolith over microservices | Accepted | 2026-07-08 | Single Spring Boot deployable, ArchUnit-enforced module boundaries, extraction seams preserved |
| [002](ADR-002-java-spring-boot-backend.md) | Java 25 LTS + Spring Boot 3.5.x backend | Accepted | 2026-07-08 | Backend language and application framework |
| [003](ADR-003-nextjs-frontend.md) | Next.js 16+ (App Router) frontend | Accepted | 2026-07-08 | Public SSR/SSG site, three portals, BFF layer |
| [004](ADR-004-postgresql-system-of-record.md) | PostgreSQL 17 as system of record | Accepted | 2026-07-08 | Relational store, FTS, JSONB specs, outbox/jobs in v1 |
| [005](ADR-005-rest-openapi.md) | REST + OpenAPI 3.1 contract-first API | Accepted | 2026-07-08 | API style, versioning, generated TypeScript client |
| [006](ADR-006-authentication-strategy.md) | Phased authentication strategy | Accepted | 2026-07-08 | Sprint 1 email/password + JWT/refresh via BFF; Phase 2 Spring Authorization Server (OAuth 2.1/OIDC) |
| 007 | Authorization model | Planned | — | Endpoint-declared permissions, org context via `X-Org-Id`, platform/org role model, 404-not-403 policy |
| 008 | Search architecture | Planned | — | `SearchService` port; PostgreSQL FTS (`tsvector` + `pg_trgm`) adapter in v1; OpenSearch/Meilisearch migration path |
| 009 | Object storage | Planned | — | S3-compatible storage (prod: R2 or S3; local: MinIO); presigned uploads via `media` module tickets |
| 010 | Internationalization | Planned | — | Locale routing in web; `localization` module serving bundles and localized entity text |
| 011 | Multi-currency | Planned | — | Minor-unit integers + explicit currency; historical exchange-rate snapshots on quotations |
| 012 | Design system | Planned | — | CSS Modules + custom properties + PostCSS design tokens; no Tailwind foundation |
| 013 | Observability | Planned | — | OpenTelemetry + Prometheus/Grafana stack + Sentry; correlation IDs propagated web→api |
| 014 | Testing strategy | Planned | — | JUnit 5 + Testcontainers + ArchUnit; Playwright + Vitest + Storybook (+ test-runner, axe) |
| 015 | Deployment strategy | Planned | — | Docker + GitHub Actions + Terraform; immutable images, Flyway on deploy, blue-green/rolling |
| 016 | CMS strategy | Planned | — | PostgreSQL-backed `content` module with CMS-shaped abstraction; no headless CMS in v1 |
| 017 | [Content-Security-Policy strategy](ADR-017-content-security-policy.md) | Accepted (interim) | 2026-07-08 | SSG-compatible CSP with strict non-script directives; nonce/hash remediation bound to Phase-4 design (SEC-DEBT-1) |
| 018 | [CSRF protection for the BFF](ADR-018-csrf-protection.md) | Accepted | 2026-07-09 | Session-bound synchronizer token (sealed in the JWE session, `sc_csrf` transport cookie, `X-CSRF-Token` header) + Origin/Fetch-Metadata/JSON-content-type depth layer; login CSRF via origin+content-type (no pre-session token exists) |
| 019 | [i18n framework, routing & negotiation](ADR-019-i18n-framework-routing.md) | Accepted | 2026-07-09 | next-intl on App Router; 20 BCP-47 locales; URL→pref→Accept-Language→en; server-resolved, per-namespace loading |
| 020 | [Multilingual DB content model & translation lifecycle](ADR-020-multilingual-content-model.md) | Accepted | 2026-07-09 | Normalized per-context `_translation` tables; MISSING→…→APPROVED lifecycle; source_version stale detection; no column-per-language |
| 021 | [RTL strategy & global typography](ADR-021-rtl-typography.md) | Accepted | 2026-07-09 | Root `dir`; CSS logical properties only; bidi isolation for LTR technical runs; script-grouped Noto font loading |
| 022 | [International SEO & machine-translation policy](ADR-022-international-seo-mt-policy.md) | Accepted | 2026-07-09 | Self-canonicalizing localized URLs; hreflang only for available translations; noindex empty/MT; MT dev-only, never authoritative |
| 023 | [Object storage & evidence upload](ADR-023-object-storage-evidence.md) | Accepted | 2026-07-09 | `ObjectStorage` port; MinIO local/CI, S3 prod; presigned staged upload + server finalize (sha256/magic-byte); deferred malware scanner, fail-closed, never fake CLEAN |
| 024 | [Optical Taxonomy module & schema architecture](ADR-024-taxonomy-architecture.md) | Accepted | 2026-07-09 | One `taxonomy` module owning one `taxonomy` schema; classification + attributes + enums + units + countries + certifications + brands + templates; extraction seams preserved |
| 025 | [Attribute registry, data types, specification templates & validation engine](ADR-025-attribute-specification-model.md) | Accepted | 2026-07-09 | Master attribute registry; closed engine-coded data-type set; category-inherited templates; pure validation engine; search/comparison metadata only |
| 026 | [Taxonomy localization & stable-identifier policy](ADR-026-taxonomy-localization-identifiers.md) | Accepted | 2026-07-09 | Codes/uuids are the only comparison key; DB `_translation` tables for admin-managed content (not message files); `en` canonical; MT never authoritative |
| 027 | [Taxonomy SEO slugs & alias resolution](ADR-027-taxonomy-seo-slugs.md) | Accepted | 2026-07-09 | Per-locale slug table, DB-enforced uniqueness, alias/redirect history; slugs are never identity; foundation only (no landing pages/routing) |

## ADR template

Every ADR in this directory follows this structure:

```markdown
# ADR-NNN: <Title>

## Status
Accepted — YYYY-MM-DD
(or: Proposed / Superseded by ADR-MMM)

## Context
What situation, constraints, and forces led to this decision.
Reference the decision matrix row and any brief mandates.

## Decision
The decision, stated actively and specifically (versions, module names,
configuration where load-bearing).

## Alternatives considered
Each alternative with a concrete reason for rejection.

## Advantages
What this decision buys us.

## Disadvantages
What we accept as a real cost.

## Risks
Each risk paired with its mitigation.

## Migration path
How we would move off this decision later — the seams, the sequence,
and the expected cost.
```

## Conventions

- Filenames: `ADR-NNN-short-slug.md`, zero-padded three-digit numbers.
- One decision per ADR. Cross-cutting details live in the referenced architecture docs.
- OpticLeague is a functional benchmark only; ADRs never frame SpexCrafters as a redesign or replacement of it.
