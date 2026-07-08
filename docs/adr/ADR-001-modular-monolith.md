# ADR-001: Modular monolith over microservices

## Status
Accepted — 2026-07-08

## Context
SpexCrafters is a global B2B optical marketplace launching with a small team and an ambitious functional surface: identity, organizations, supplier verification, product catalog with attribute-driven specs, search, RFQ/quotation workflows, messaging, notifications, media, content, SEO, localization, multi-currency, analytics, audit, and administration — twenty-plus bounded contexts (system-architecture.md §G.3). Traffic at launch does not justify distributed-systems overhead, but the domain decomposition must be real from day one so that individual contexts (most plausibly `search`, `media`, `messaging`) can be extracted when scale or ownership pressure demands it.

## Decision
Build the backend as a **single deployable Spring Boot application** (`apps/api`) organized as a Maven multi-module modular monolith: `apps/api/modules/<context>` plus `shared-kernel`, package root `com.spexcrafters.<context>`, package-by-feature inside each module. Boundaries are enforced mechanically, not by convention:

1. A module's only public surface is its `.api` package (application services + DTOs) and its published domain events; `.internal` is inaccessible cross-module (ArchUnit rules in CI).
2. No cross-module JPA relationships or repository/table access; cross-context references are typed IDs (`OrganizationId`), with DB FKs where the schema allows.
3. Acyclic dependency direction per the module table; `shared-kernel` depends on nothing.
4. Cross-module side effects flow through **domain events over a transactional outbox** (`event_outbox` table), e.g. `QuotationSubmitted` → notifications + search + analytics.
5. Async work (emails, image derivatives, search-index refresh, saved-search alerts) runs on Spring scheduling + a PostgreSQL-backed `job_queue`, with Redis locks coordinating multi-instance execution. No message broker in v1.

The app is stateless and horizontally scalable; sessions/tokens are externalized.

## Alternatives considered
- **Microservices from day one** — rejected: operational cost (service discovery, distributed tracing as a necessity rather than nicety, per-service CI/CD, network failure modes, data consistency across stores) without any scale justification; would slow a small team drastically and multiply infrastructure spend.
- **Message broker (Kafka/RabbitMQ) alongside the monolith** — rejected for v1: the PostgreSQL outbox + polled workers deliver the same decoupling semantics at current volume with zero extra infrastructure; the event contracts are broker-ready when volume justifies one.
- **Unstructured monolith ("big ball of mud")** — rejected: cheapest today, ruinous later; without enforced seams the extraction path disappears and cross-context coupling accretes silently.
- **Serverless/function decomposition** — rejected: cold starts, per-invocation limits, and fragmented transaction boundaries fit poorly with JPA-based aggregates and long-lived connection pools.

## Advantages
- One deployable, one database transaction boundary: cross-context workflows (RFQ publish → notify invited suppliers → project into search) are simple and consistent.
- Single CI/CD pipeline, single observability surface, trivially reproducible local environment (Docker Compose: postgres, redis, minio, mailpit).
- Refactoring across module boundaries is a compiler-checked operation, not a cross-repo negotiation.
- Real domain boundaries (ArchUnit-verified) keep future extraction cheap: OpenAPI boundary + event contracts are the seams.

## Disadvantages
- Whole-application deploys: a change in `content` redeploys `rfq`. Mitigated by fast pipelines and blue-green/rolling deploys, but the coupling is real.
- Shared JVM: a runaway query or memory leak in one module affects all. Resource isolation requires discipline (query budgets, timeouts) rather than process boundaries.
- Scaling is uniform; we cannot give `search` more replicas than `messaging` until extraction.

## Risks
- **Boundary erosion under deadline pressure** → ArchUnit tests fail the build on `.internal` access, cross-module JPA relations, or dependency-cycle introduction; per-context DB schemas (`identity.*`, `catalog.*`, …) make ownership physical.
- **Outbox/job table becomes a bottleneck at volume** → indexes and partitioning on `event_outbox`/`job_queue`; polled-worker batch sizes tuned; broker adoption is a consumer-side swap because producers only write the outbox.
- **Team scales past the monolith's coordination limit** → module ownership maps to team ownership; extraction path (below) is rehearsed, not theoretical.

## Migration path
Extraction of a module (expected first candidates: `search`, `media`, `messaging`) proceeds without a rewrite:
1. The module already exposes only its `.api` services and events — introduce an HTTP/gRPC adapter implementing the same `.api` interfaces.
2. Move its schema (`search.*` etc.) to a dedicated database; cross-schema FKs to it were already forbidden in code, so only data migration remains.
3. Replace outbox polling with a broker (Kafka/RabbitMQ) for that module's event streams; contracts are unchanged.
4. Deploy the module as its own container behind the same API gateway; the OpenAPI surface (`/api/v1`) is unaffected.
Cost is bounded to adapter code + data migration because ADR-001's enforcement rules kept the seams clean.
