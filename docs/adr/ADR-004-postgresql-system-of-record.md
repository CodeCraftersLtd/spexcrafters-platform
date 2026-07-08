# ADR-004: PostgreSQL 17 as system of record

## Status
Accepted — 2026-07-08

## Context
SpexCrafters v1 needs, simultaneously: a relational system of record for transactional workflows (RFQ → quotation → award, org membership, verification); flexible attribute storage for optical product specs (lens refractive index, Abbe value, coatings; frame dimensions; machinery capacity — the attribute registry varies per category); full-text and fuzzy search over products, suppliers, and RFQs; and durable queues for the outbox/job pattern from ADR-001. A small team cannot responsibly operate a relational database plus a search cluster plus a document store plus a message broker at launch. The longevity policy mandates a managed service and a "latest stable major − 1" version policy.

## Decision
**PostgreSQL 17** (managed service in production) is the single data platform for v1, covering four roles:

1. **System of record** — one database, schema-per-bounded-context (`identity.*`, `catalog.*`, `rfq.*`, …) making module ownership physical; cross-schema FKs allowed for integrity, cross-module table access forbidden in code (ArchUnit). UUIDv7 PKs; audit columns and optimistic-locking `version` on concurrently-edited aggregates; money as `amount_minor bigint + currency char(3)`, never floats.
2. **Flexible specs** — JSONB columns validated against the `catalog` attribute registry for per-category product attributes, avoiding EAV sprawl while keeping GIN-indexable filters.
3. **Search** — generated `tsvector` columns + GIN indexes for FTS; `pg_trgm` GIN indexes for fuzzy name matching; a denormalized `search_documents` projection refreshed by domain events, accessed only through the `SearchService` port (ADR-008 scope).
4. **Async backbone** — `event_outbox` and `job_queue` tables with polled workers (Redis locks for multi-instance coordination).

Schema changes are **Flyway-only** (`ddl-auto=validate`), expand → migrate → contract for zero downtime. Backups: daily base + WAL for PITR, encrypted, offsite copy, quarterly restore rehearsal (RPO ≤ 15 min, RTO ≤ 4 h).

## Alternatives considered
- **MySQL** — rejected: materially weaker full-text search and JSON indexing/expression support; no equivalent of `pg_trgm`; the v1 strategy of "one database covers search + flexible attributes" doesn't hold on MySQL.
- **PostgreSQL + dedicated search engine (Elasticsearch/OpenSearch/Meilisearch) from day one** — rejected for v1: a second stateful cluster with its own sync, relevance-tuning, and upgrade burden before search volume justifies it; the `SearchService` port keeps this a cheap later migration (ADR-008).
- **Document store (MongoDB) for product specs** — rejected: the domain is overwhelmingly relational (orgs, memberships, RFQ state machines, FK integrity); JSONB delivers the flexible-attributes need without splitting the system of record.
- **Separate message broker for async work** — rejected for v1 per ADR-001: transactional outbox in the same database gives exactly-once-producer semantics free with local transactions.

## Advantages
- One store to operate, back up, secure, and reason about; transactions span business writes and their outbox events atomically.
- FTS + trigram + JSONB + LISTEN-capable queues cover four infrastructure needs with zero additional services.
- Managed PostgreSQL is a commodity: providers are interchangeable, expertise is abundant, and the 10+ year longevity outlook is as safe as database bets get.
- Schema-per-context maps the modular monolith onto the storage layer, keeping extraction seams physical.

## Disadvantages
- Single-store concentration: search load, job polling, and OLTP contend for the same instance.
- PG FTS relevance and faceting are cruder than a dedicated engine; acceptable for phase-1 catalog scale, not forever.
- JSONB attributes shift some validation burden to the application (attribute registry checks) that a rigid schema would carry.

## Risks
- **Search/OLTP contention as catalog grows** → read replicas for search and heavy reads first; `search` module extraction (own store or dedicated engine) is the rehearsed second step; top-20 query budget list checked with `EXPLAIN ANALYZE` in CI.
- **Job/outbox tables bloating under write volume** → monthly partitioning (`login_attempt` already partitioned; same pattern), aggressive vacuum settings, archived-row purging jobs.
- **Version drift vs. policy** → "latest stable major − 1" (17 while 18 matures) with managed-service minor upgrades automatic; Testcontainers pins the same major in CI so tests run against production truth.
- **Data loss** → PITR backups with quarterly restore rehearsal; migration CI runs from empty AND from the latest-release schema snapshot.

## Migration path
- **Search extraction (most likely first):** all search access already flows through the `SearchService` port and the event-refreshed `search_documents` projection; an OpenSearch/Meilisearch adapter re-consumes the same events — no caller changes.
- **Read scaling:** managed read replicas, routed per-module via repository configuration.
- **Module store extraction:** schema-per-context means a module's tables can move to a dedicated database with the module (ADR-001 path); cross-schema FKs to it are replaced by event-driven consistency, which the outbox already provides.
- **Full engine replacement** (unlikely): Flyway history is portable SQL-first; the ORM layer (Hibernate) abstracts dialect specifics except FTS/JSONB usages, which are isolated in `search` and `products` infrastructure adapters.
