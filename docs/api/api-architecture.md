# J — API Architecture

**Project:** SpexCrafters · **Date:** 2026-07-08 · **Style:** REST, OpenAPI 3.1, contract-first discipline

## J.1 Conventions

| Topic | Convention |
|---|---|
| Base path | `https://api.spexcrafters.com/api/v1` (v1 in path; see versioning) |
| Resources | Plural nouns, kebab-case: `/products`, `/supplier-profiles`, `/rfqs/{id}/quotations` |
| IDs | UUIDv7 strings; human reference numbers (`rfqNo`) are display fields, never path keys |
| Methods | GET (safe), POST (create/actions), PUT (full replace, rare), PATCH (partial, JSON Merge Patch), DELETE (archive/soft per domain) |
| Field names | camelCase JSON; ISO-8601 UTC timestamps; money as `{ "amountMinor": 125000, "currency": "USD" }` |
| Localization | `Accept-Language` negotiated, `?locale=` override; localized fields returned resolved + `availableLocales` |
| Compression/caching | ETag + conditional GET on public catalog resources; `Cache-Control` public with short TTL on catalog reads, private no-store on portal reads |

## J.2 Contract & client generation

- Springdoc generates the OpenAPI 3.1 document from controllers + annotations; the exported `openapi.json` is **committed to `packages/api-client/spec/`** and diffed in CI (breaking-change detector; fails on incompatible change without a version bump note).
- `packages/api-client` builds a typed TS client (`openapi-typescript` types + thin `openapi-fetch` wrapper). Frontend imports only this package — manual duplication of backend types is banned by lint rule.
- Contract tests: backend integration tests validate responses against the schema; frontend CI compiles against the regenerated client.

## J.3 Versioning & deprecation

- Path major version (`/api/v1`). Additive changes (new optional fields/endpoints) don't bump. Breaking changes require `/api/v2` coexisting ≥ 6 months with `Deprecation` + `Sunset` headers on v1 and changelog entries.
- Enums are open: clients must tolerate unknown enum values (documented in client README, enforced by generated union types + fallback).

## J.4 Pagination, filtering, sorting

- **Cursor (keyset) pagination** for all feeds: `?cursor=<opaque>&limit=24` → `{ items, nextCursor, prevCursor?, total? }` (total only where cheap/needed). Offset pagination only on bounded admin tables.
- Filtering: flat query params mapped to the attribute registry (`?category=ophthalmic-lenses&spec.lens.refractiveIndex=1.67&country=CN&verified=true&moqMax=500`). The same canonical serialization drives shareable frontend URLs.
- Sorting: `?sort=relevance|-createdAt|moq|leadTime` (whitelisted per endpoint).

## J.5 Errors

RFC 9457 `application/problem+json`:

```json
{
  "type": "https://api.spexcrafters.com/problems/validation",
  "title": "Validation failed",
  "status": 422,
  "detail": "2 fields are invalid.",
  "instance": "/api/v1/rfqs",
  "correlationId": "01J...",
  "errors": [
    { "field": "quantity", "code": "min", "message": "Must be at least 1." },
    { "field": "spec.lens.refractiveIndex", "code": "enum", "message": "Unknown value." }
  ]
}
```

Catalogued problem types: `validation` 422 · `authentication` 401 · `authorization` 403 · `not-found` 404 · `conflict` 409 (incl. optimistic-lock `version` conflicts) · `rate-limited` 429 (+`Retry-After`) · `payload-too-large` 413 · `internal` 500 (no leak; correlation ID only). Error copy keys map to frontend i18n.

## J.6 Authentication & sessions

- **BFF pattern:** browser ↔ Next.js with HttpOnly, Secure, SameSite=Lax session cookie; Next.js server exchanges/refreshes OAuth 2.1 Authorization-Code+PKCE tokens with the Spring Authorization Server and calls the API with bearer tokens. Browser never holds tokens.
- Server-to-server (future ERP integrations): OAuth client-credentials with scoped machine clients.
- CSRF: state-changing BFF routes require the SameSite cookie + double-submit token; the pure-bearer API path is CSRF-immune by construction.
- Session lifetimes: access 10 min / refresh sliding 14 days (portal), 2 h absolute for `/admin` scope; MFA step-up required for admin scope and sensitive org actions (ownership transfer).

## J.7 Authorization

Every endpoint declares permissions (see [roles-and-permissions.md](../architecture/roles-and-permissions.md)); org context via `X-Org-Id` header validated against membership on every request; resource-ownership checks in services. 404-not-403 policy for resources whose existence is itself sensitive (private RFQs, conversations).

## J.8 Idempotency & concurrency

- `Idempotency-Key` header honored on POST for RFQ publication, quotation submission/revision, message send, media-ticket creation: key + request-hash stored 24 h; replays return the original response (409 on same key/different payload).
- Optimistic concurrency: mutable aggregates return `version`; PATCH requires it; mismatch → 409 `conflict` with current state reference.

## J.9 Rate limiting & abuse controls

Redis token buckets, layered: per-IP on anonymous reads (generous), per-user on authenticated calls, strict per-user+per-resource on expensive/abusable ops (search autocomplete, message send, RFQ create, login/OTP attempts with exponential backoff + lockout). Responses expose `RateLimit-*` headers. Edge/WAF provides the first coarse layer.

## J.10 Endpoint inventory (v1 surface, summary)

| Area | Endpoints (representative) |
|---|---|
| Auth (BFF + AS) | `POST /auth/register`, `POST /auth/verify-email`, `POST /auth/password-reset[-confirm]`, OIDC endpoints, `POST /auth/mfa/totp[/confirm]`, `GET /me`, `GET /me/organizations` |
| Organizations | `POST /organizations`, `GET/PATCH /organizations/{id}`, `POST …/invitations`, `POST /invitations/{token}/accept`, `GET/PATCH …/members/{id}`, `POST …/verification-requests` (+ documents) |
| Catalog (public) | `GET /categories[/tree|/{slug}]`, `GET /attributes?category=`, `GET /brands[/{slug}]` |
| Products | `GET /products` (facets via `GET /products/facets`), `GET /products/{slug}`, supplier-side `POST/PATCH /supplier/products`, `POST …/media`, `POST …/publish` |
| Suppliers | `GET /suppliers`, `GET /suppliers/{slug}`, supplier-side `PATCH /supplier/profile`, certifications CRUD |
| Search | `GET /search?q=` (multi-entity), `GET /search/suggest?q=` (autocomplete), `POST /saved-searches`, `GET/DELETE /saved-searches/{id}` |
| RFQ | `POST /rfqs` (draft), `POST /rfqs/{id}/publish`, `GET /rfqs` (board, qualification-filtered), `GET /rfqs/{id}`, `POST /rfqs/{id}/invitations`, `POST /rfqs/{id}/award`, `POST /rfqs/{id}/close` |
| Quotations | `POST /rfqs/{id}/quotations`, `POST /quotations/{id}/revisions`, `POST /quotations/{id}/withdraw`, `GET /rfqs/{id}/quotations` (buyer), `GET /supplier/quotations` |
| Messaging | `GET/POST /conversations`, `GET /conversations/{id}/messages?cursor=`, `POST …/messages`, `POST …/read`, `POST /orgs/{id}/block`, `POST /abuse-reports` |
| Favorites | `PUT/DELETE /favorites/{type}/{id}`, `GET /favorites` |
| Notifications | `GET /notifications?cursor=`, `POST /notifications/read`, `GET /notifications/stream` (SSE, phase 6+) |
| Media | `POST /media/upload-tickets` → presigned PUT → `POST /media/{id}/confirm` |
| Content (public) | `GET /pages/{slug}`, `GET /insights`, `GET /insights/{slug}`, `GET /events` |
| Currency/locale | `GET /currencies`, `GET /exchange-rates?base=&quote=&date=`, `GET /locales` |
| Admin | Mirrors under `/admin/*` with granular permissions: users, orgs, verification queue + decisions, moderation queues, taxonomy CRUD, content/homepage, seo/redirects, templates, audit-log search, flags, config |
| Ops | `GET /actuator/health|ready|live` (internal), `GET /actuator/prometheus` (scrape network only) |

## J.11 Security headers & transport

TLS-only (HSTS preload), CSP (nonce-based, no `unsafe-inline`), `X-Content-Type-Options`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy` minimal; CORS allowlist = web origins only (API is not a public CORS API in v1). Request size caps; multipart uploads only via presigned storage URLs, never through the API process.
