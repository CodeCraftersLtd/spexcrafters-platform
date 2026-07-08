# ADR-005: REST + OpenAPI 3.1 contract-first API

## Status
Accepted — 2026-07-08

## Context
`apps/web` and `apps/api` are separate deployables in different languages; without a machine-checked contract, backend and frontend types drift and integration breaks silently. The public catalog surface must be CDN-cacheable (ETag/`Cache-Control` on product, supplier, and content reads) for SEO-scale traffic. Future consumers — ERP/commerce integrations for SpexCrafters' own suppliers — will use the same public contract. The API shape is resource-centric (products, suppliers, RFQs, quotations, conversations) with well-understood read/write patterns, not a graph of arbitrary client-composed queries.

## Decision
- **REST over HTTPS, documented as OpenAPI 3.1**, under `https://api.spexcrafters.com/api/v1` (major version in path).
- **Contract generation and enforcement:** Springdoc generates the OpenAPI 3.1 document from controllers/annotations; the exported `openapi.json` is committed to `packages/api-client/spec/` and diffed in CI with a breaking-change detector that fails on incompatible changes lacking a version-bump note. `packages/api-client` builds the typed TS client (`openapi-typescript` types + thin `openapi-fetch` wrapper); the frontend may import only this package — manual duplication of backend types is banned by lint rule. Backend integration tests validate responses against the schema; frontend CI compiles against the regenerated client.
- **Conventions** (full detail in api-architecture.md §J): plural kebab-case resources; UUIDv7 path IDs; camelCase JSON; money as `{ amountMinor, currency }`; RFC 9457 `application/problem+json` errors with catalogued problem types and correlation IDs; cursor (keyset) pagination for all feeds; `Accept-Language` localization; `Idempotency-Key` on abusable POSTs; optimistic-concurrency `version` on PATCH.
- **Versioning:** additive changes never bump; breaking changes ship `/api/v2` coexisting ≥ 6 months with `Deprecation`/`Sunset` headers on v1. Enums are open — clients must tolerate unknown values (enforced by generated union types + fallback).

## Alternatives considered
- **GraphQL** — rejected: HTTP/CDN caching of public catalog pages is central to the SEO strategy and is awkward with POST-based graph queries; resolver complexity, N+1 discipline, and schema-governance overhead buy flexibility this resource-shaped product doesn't need; the OpenAPI→TS generation pipeline already kills type duplication, which was GraphQL's main draw.
- **tRPC** — rejected: couples the contract to TypeScript on both ends; our backend is Java (ADR-002), and future ERP consumers won't be TS. The contract must be language-neutral.
- **gRPC (public surface)** — rejected: poor browser/CDN story; wrong tool for a public, cacheable, SEO-adjacent API. Remains an option for internal module extraction transport later.
- **Hand-maintained REST without a committed spec** — rejected: this is the drift scenario the decision exists to prevent; "documentation" that CI doesn't verify is fiction within a quarter.

## Advantages
- One committed artifact (`openapi.json`) is simultaneously documentation, the TS client source, the contract-test oracle, and the CI breaking-change gate.
- Plain REST semantics give free CDN/ETag caching on public reads and a familiar surface for future third-party integrators.
- OpenAPI 3.1 aligns with JSON Schema, so Zod form schemas can be derived from the same types where practical.
- The spec outlives any tooling: generators are swappable; the contract is the 10-year asset.

## Disadvantages
- REST under-fetches/over-fetches relative to client-composed queries; portal screens sometimes need multiple requests or purpose-built aggregate endpoints.
- Code-first spec generation means contract quality depends on annotation discipline; a sloppy controller produces a sloppy contract.
- Versioning-by-path makes breaking changes expensive by design — deliberate, but real friction.

## Risks
- **Generator quality (Springdoc output vs. openapi-typescript input)** → the generated spec and client are validated in CI on every build; known-problem constructs (polymorphism, maps) get explicit schema annotations and contract tests.
- **Accidental breaking changes** → CI spec diff with breaking-change detection is a hard gate; open-enum policy prevents the most common silent break.
- **Endpoint sprawl / convention drift** → conventions codified in api-architecture.md §J; representative endpoint inventory (§J.10) reviewed at design time; problem-type catalogue is closed and extended deliberately.

## Migration path
- **Tooling swaps are cheap:** Springdoc, openapi-typescript, and openapi-fetch are each replaceable independently because the committed spec is the interface between them.
- **Spec-first inversion:** if code-first annotation quality becomes the bottleneck, flip to authoring `openapi.json` by hand and validating controllers against it — the CI harness already compares both directions.
- **Adding GraphQL/BFF aggregation later:** a read-only aggregation layer can be built in the Next.js BFF or as a gateway over the same REST API without disturbing the contract.
- **v2 evolution:** the path-versioning + 6-month coexistence policy is itself the migration mechanism for contract-level change; `Deprecation`/`Sunset` headers and changelog entries give integrators a predictable runway.
