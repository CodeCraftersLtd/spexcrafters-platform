# G — System Architecture (Modular Monolith)

**Project:** SpexCrafters · **Date:** 2026-07-08

## G.1 System context (C4 level 1)

```
                        ┌──────────────────────────────────────────────┐
  Buyers ─────browser──▶│             SpexCrafters Platform            │──SMTP──▶ Email provider
  Suppliers ──browser──▶│                                              │──HTTPS─▶ Exchange-rate provider
  Admins ─────browser──▶│                                              │──HTTPS─▶ Error monitoring (Sentry)
  Search engines ──────▶│  (public SSR pages)                          │──OTLP──▶ Observability backend
                        └──────────────────────────────────────────────┘          (Grafana/Prometheus/Tempo/Loki)
  Future: ERP / commerce integrations via the same public API contract
```

## G.2 Container architecture (C4 level 2)

```
            CDN + edge (TLS, caching, WAF/rate-limit first line)
                              │
        ┌─────────────────────┴──────────────────────┐
        ▼                                            ▼
┌──────────────────┐   REST /api/v1 (OpenAPI)  ┌──────────────────┐
│  apps/web        │ ─────────────────────────▶│  apps/api        │
│  Next.js 16+     │   (server-to-server +     │  Spring Boot     │
│  App Router, RSC │    BFF session cookies)   │  modular monolith│
│  BFF route       │                           │  Java 25 LTS     │
│  handlers (auth, │                           └───┬────┬────┬────┘
│  uploads proxy)  │                               │    │    │
└──────────────────┘                               ▼    ▼    ▼
                                          PostgreSQL  Redis  S3-compatible
                                          (system of  (rate  object storage
                                           record,    limit, (media, docs)
                                           FTS/trgm)  cache,
                                                      locks)
```

- **apps/web** renders all public pages (SSR/SSG/ISR) and the three portals; it never talks to PostgreSQL directly.
- **apps/api** is a single deployable Spring Boot application containing all bounded contexts; horizontal-scalable (stateless; sessions/tokens externalized).
- **Async work** (emails, notifications, image derivatives, search-index refresh, saved-search alerts) runs on Spring's scheduling + a PostgreSQL-backed outbox/job table in v1 — no message broker until volume justifies one. Redis coordinates locks for multi-instance job execution.

## G.3 Bounded contexts / backend modules

Maven multi-module layout: `apps/api/modules/<context>` + `shared-kernel`, package root `com.spexcrafters.<context>`, **package-by-feature inside each module**.

| Module | Owns (aggregate roots) | Depends on (API only) | Publishes domain events |
|---|---|---|---|
| `shared-kernel` | Money, Locale, Slug, AuditStamp, DomainEvent base, error model | — | — |
| `identity` | User, Credential, MfaEnrolment, Session/Token, LoginAttempt | — | UserRegistered, EmailVerified, SuspiciousLogin |
| `organizations` | Organization, Membership, Invitation, OrgRole | identity | OrganizationCreated, MemberJoined |
| `verification` | VerificationRequest, VerificationDocument, Decision | organizations, media | OrgVerified, VerificationRejected |
| `suppliers` | SupplierProfile, Capability, Certification, FactoryEvidence | organizations, media | SupplierProfilePublished |
| `buyers` | BuyerProfile, Address | organizations | — |
| `catalog` | Category, AttributeDefinition, AttributeTemplate, Brand | — | CategoryChanged |
| `products` | Product, Variant, PriceTier, ProductMedia, SpecSheet | catalog, suppliers, media | ProductPublished, ProductArchived |
| `search` | SearchDocument projections, SavedSearch | (consumes events from products/suppliers/rfq) | SavedSearchMatched |
| `rfq` | Rfq, RfqItem, RfqInvitation, RfqAttachment | organizations, catalog, media | RfqPublished, RfqAwarded, RfqClosed |
| `quotations` | Quotation, QuotationRevision, QuotationDocument | rfq, organizations, currency, media | QuotationSubmitted, QuotationRevised, QuotationWithdrawn |
| `messaging` | Conversation, Message, Attachment, Block, MessageReport | organizations, rfq, products, media | MessageSent |
| `notifications` | Notification, NotificationTemplate, EmailOutbox | (consumes all events) | — |
| `favorites` | Favorite (product/supplier), — | products, suppliers | — |
| `media` | MediaAsset, Derivative, UploadTicket | — | MediaScanned |
| `content` | Page, Article, Event, HomepageSection, Banner | media | ContentPublished |
| `seo` | MetadataOverride, Redirect, SitemapSegment | (reads products/suppliers/content) | — |
| `localization` | Locale, TranslationBundle, LocalizedText | — | — |
| `currency` | Currency, ExchangeRate (historical) | — | RatesUpdated |
| `analytics` | UsageEvent projections, supplier metrics | (consumes events) | — |
| `audit` | AuditLogEntry, SecurityEvent | (consumes all) | — |
| `administration` | ModerationCase, AbuseReport, FeatureFlag, PlatformConfig | everything (via module APIs) | OrgSuspended |

### Module interaction rules (enforced by ArchUnit)

1. A module's only public surface is its `…​.api` package (application services + DTOs) and its published domain events. `…​.internal` is inaccessible cross-module.
2. **No cross-module JPA relationships and no cross-module repository/table access.** Foreign references across contexts are held as typed IDs (e.g. `OrganizationId`), with referential integrity via DB FKs where the schema allows and consistency via events elsewhere.
3. Dependency direction is acyclic per the table above; `shared-kernel` depends on nothing.
4. Cross-module workflows with side effects use **domain events over a transactional outbox** (e.g. `QuotationSubmitted` → notifications + search + analytics), keeping producers ignorant of consumers.
5. Controllers contain no business logic; services own transactions; domain objects own invariants.

## G.4 Layering inside a module

```
<context>/
  api/            application services (module public API), command/query DTOs
  domain/         aggregates, value objects, domain services, events, invariants
  infrastructure/ JPA repositories/entities-mapping, external adapters
  web/            REST controllers (thin), request/response models, mappers
```

JPA entities never cross the `web` boundary; MapStruct (or explicit mappers) produce response DTOs.

## G.5 Frontend architecture

- **Server Components by default**; Client Components only for interactivity (facet panel, quote composer, messaging, dashboards).
- Data access: public pages fetch the REST API from the server (cached per route semantics: SSG + ISR for catalog/content, SSR for personalized); portals use the generated TypeScript client, with TanStack Query only in genuinely interactive client islands (messages, notifications, tables).
- **BFF pattern:** Next.js route handlers own the OIDC dance (Authorization Code + PKCE), hold tokens server-side, and issue HttpOnly SameSite cookies to the browser; the browser never sees access tokens.
- Styling: CSS Modules + design tokens from `packages/design-tokens`; no Tailwind foundation.
- Forms: React Hook Form + Zod schemas (Zod schemas derived from OpenAPI types where practical).

## G.6 Cross-cutting decisions

| Concern | Decision |
|---|---|
| Search | `SearchService` port; PostgreSQL FTS (`tsvector` + `pg_trgm`) adapter in v1; denormalized `search_documents` projection refreshed by events; swappable to OpenSearch/Meilisearch later |
| Caching | HTTP/CDN caching for public pages; Redis only for rate limits, locks, short-TTL hot lookups (exchange rates, config) — every cache entry has an explicit invalidation trigger |
| Files | Presigned upload to S3-compatible storage via `media` module tickets; type/size validation, metadata stripping, derivative pipeline (WebP/AVIF), private ACLs for verification docs/attachments; MinIO locally |
| Background jobs | `job_queue` + `event_outbox` tables, polled workers, Redis lock per job type; idempotent handlers |
| Observability | OpenTelemetry SDK both apps; structured JSON logs with correlation IDs propagated web→api; Prometheus metrics; health/readiness/liveness endpoints; Sentry for errors; CWV RUM on web |
| Multi-currency | Minor-unit integers + explicit currency; historical rate snapshots on quotations (see domain model §I) |
| i18n | Locale routing in web; `localization` module serves bundles + localized entity text |
| Extraction path | Any module with independent scale/ownership pressure (likely `search`, `media`, `messaging`) can be extracted behind its existing API + events; the OpenAPI boundary and event contracts are the seams |

## G.7 Environments & deployment (summary — details in roadmap/ADR-015)

Local (Docker Compose: postgres, redis, minio, mailpit) → Development → Staging → Production. Immutable container images, GitHub Actions CI/CD, Flyway migrations run on deploy (backward-compatible, expand→migrate→contract), blue-green or rolling for zero downtime, managed PostgreSQL/Redis/object storage in production, Terraform for cloud resources.
