# O — First Implementation Sprint (proposal, pending approval)

**Project:** SpexCrafters · **Date:** 2026-07-08 · **Status:** APPROVED (stakeholder direction 2026-07-08, following brand correction)
**Sprint goal:** *A stranger can clone the repo, run one command, register an account, verify email, log in with a session cookie, and see a token-styled placeholder homepage — with CI proving all of it on every commit.*
This is Phase 3 "Foundation" sliced to its walking skeleton. **No sprint work begins until deliverables A–O are approved** (this document included).

## Scope (committed)

### 1. Repository & tooling
- Initialize monorepo per [repository-structure.md](repository-structure.md); move workspace **out of OneDrive** to a local dev path (risk M.3).
- pnpm workspaces; `packages/config` (eslint/tsconfig/prettier/stylelint strict presets); Conventional Commits + gitleaks hook; `.env.example`.

### 2. Local environment
- `infrastructure/docker/compose.yaml`: PostgreSQL, Redis, MinIO, Mailpit, otel-collector (+ Grafana/Prometheus optional profile). `pnpm dev` / `make dev` bootstraps everything with seeded dev config.

### 3. Backend walking skeleton
- Spring Boot app + Maven module skeleton: `shared-kernel`, `identity`, `organizations` (stub), `audit` (stub); ArchUnit boundary suite active from day one.
- Flyway `V1__identity_baseline.sql` (user_account, tokens, login_attempt, audit_log) per [domain-model.md](database/domain-model.md); `ddl-auto=validate`.
- Identity vertical slice: register → email verification (Mailpit) → login → refresh; Argon2id hashing; rate-limited login attempts (Redis bucket); Spring Authorization Server issuing code+PKCE tokens for the web client.
- Actuator health/ready/live; OTel traces + JSON logs with correlation IDs; problem+json error handler; Springdoc exporting `openapi.json` in build.

### 4. Frontend walking skeleton
- Next.js app: locale routing (`en` + placeholder bundles for zh-Hans/fr/de), route groups, root layouts, security headers, metadata baseline, designed 404/500.
- BFF auth: route handlers for OIDC code+PKCE exchange, HttpOnly session cookie, `/auth/login`, `/auth/register`, `/auth/verify-email` pages using React Hook Form + Zod against generated client.
- `packages/design-tokens` v0 (from deliverable L: color/type/space/motion primitives, build to CSS vars + TS) and `packages/ui` v0: Button, Input, FormField, Alert — CSS Modules, Storybook with axe checks.
- Placeholder homepage rendering tokens (no art direction yet — hero comes with Phase 4 after design-system sprint).

### 5. Contract & CI/CD
- `packages/api-client` generation pipeline + committed spec + breaking-change diff job.
- GitHub Actions: web (lint/typecheck/unit/build + Lighthouse budget on `/`), api (unit/integration with Testcontainers/ArchUnit/migration-from-empty), contract job, security job (dependency + container scan, gitleaks), Playwright smoke E2E: **registration → email verify → login → authenticated hello**.
- Dockerfiles for both apps; images build in CI.

## Out of scope (explicitly)
Catalog, search, RFQ, portals, admin, MFA (next sprint), design-direction homepage, any production infrastructure (Terraform lands when a target cloud account exists).

## Definition of Done (sprint)
Global DoD (roadmap M.0) plus: fresh-clone bootstrap ≤ 15 min documented in README; smoke E2E green in CI; OpenAPI→client roundtrip proven by the register/login pages; zero hardcoded user-facing strings; ADR-001…006 merged (monolith, backend, frontend, PostgreSQL, REST/OpenAPI, auth) — drafted from [technology-decisions.md](architecture/technology-decisions.md).

## Sequencing & estimate

| Order | Work item | Est. |
|---|---|---|
| 1 | Repo scaffold + config + compose | 2 d |
| 2 | Backend skeleton + Flyway V1 + ArchUnit | 3 d |
| 3 | Identity slice + Authorization Server | 4 d |
| 4 | Tokens v0 + ui v0 + Storybook | 3 d (parallel with 3) |
| 5 | Next.js skeleton + BFF + auth pages | 4 d |
| 6 | Contract pipeline + CI + Dockerfiles + smoke E2E | 3 d |
| — | **Total ≈ 3 team-weeks** (2 eng parallelizing) | |

## Stakeholder inputs
1. ~~Brand identity~~ **RESOLVED 2026-07-08:** the platform is **SpexCrafters**; OpticLeague is strictly a competitor; no data migration or continuity workstream exists.
2. ~~Design direction~~ **RESOLVED 2026-07-08:** "MERIDIAN with a spectral signature" retained as the SpexCrafters design direction; token prefix `--sc-*`.
3. Domain + email-sending provider (SPF/DKIM for verification emails in staging/production). **Working assumption until confirmed:** `spexcrafters.com`; dev uses Mailpit, no provider needed.
4. Target cloud provider preference (defers Terraform correctly). Sprint 1 proceeds cloud-agnostically.
5. Pricing-visibility policy (OQ-7) — needed by Phase 4; price DTOs carry a per-tier `visibility` field either way.
