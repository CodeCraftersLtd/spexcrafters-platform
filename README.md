# SpexCrafters

**The global B2B sourcing, discovery, verification, and procurement infrastructure for the optical industry.**

SpexCrafters connects optical lens and frame manufacturers, laboratories, coating labs, machinery and equipment suppliers, accessories and packaging manufacturers, distributors, wholesalers, optical chains, retailers, opticians, optometrists, and international procurement teams — through a verified supplier directory, specification-grade product catalog, and structured RFQ/quotation workflows.

> ⚠️ **Move this repository out of OneDrive before development.** Cloud-sync file locking corrupts `node_modules`, Maven `target/`, and Next.js build caches. Clone/move to a local path (e.g. `C:\dev\spexcrafters`) first.

## Repository layout

| Path | Contents |
|---|---|
| [`apps/web`](apps/web) | Next.js 16 public website + buyer/supplier/admin portals (App Router, TS strict, CSS Modules, BFF auth) |
| [`apps/api`](apps/api) | Java 25 / Spring Boot modular monolith (Maven multi-module, Flyway, ArchUnit-enforced bounded contexts) |
| [`packages/design-tokens`](packages/design-tokens) | `--sc-*` design tokens (JSON source → CSS custom properties + TS constants) |
| [`packages/ui`](packages/ui) | Design-system React components ("MERIDIAN with a spectral signature") + Storybook |
| [`packages/api-client`](packages/api-client) | Committed OpenAPI 3.1 contract + generated TypeScript client |
| [`packages/config`](packages/config) | Shared tsconfig/ESLint/Prettier/Stylelint presets |
| [`infrastructure/docker`](infrastructure/docker) | Local Compose stack + production Dockerfiles |
| [`docs`](docs/README.md) | Architecture, design system, ADRs, roadmap — start at [docs/README.md](docs/README.md) |

## Prerequisites

- **Node.js 22 LTS** + **pnpm 9** (`corepack enable`)
- **JDK 25 (Temurin)** + **Maven 3.9+**
- **Docker Desktop** (Compose v2; also required for Testcontainers integration tests)

## Quick start

```bash
cp .env.example .env                 # then generate real SESSION_SECRET / JWT secret (see file comments)
pnpm install
pnpm dev:infra                       # PostgreSQL, Redis, MinIO, Mailpit
pnpm tokens:build
pnpm dev:api                         # Spring Boot on :8080 (runs Flyway migrations)
pnpm dev:web                         # Next.js on :3000
```

Then: register at `http://localhost:3000/en/auth/register`, open the verification email in **Mailpit** (`http://localhost:8025`), verify, and sign in.

## Verification commands

```bash
pnpm lint && pnpm typecheck && pnpm test   # frontend workspaces
mvn -f apps/api/pom.xml verify              # backend unit + Testcontainers + ArchUnit
pnpm e2e                                    # Playwright smoke (needs full stack up)
```

CI mirrors these in [.github/workflows](.github/workflows) (`ci-web`, `ci-api`, `contract`, `security`, `e2e-smoke`).

## Ground rules

- OpticLeague is a **competitor benchmark only** — no competitor code, content, branding, or datasets exist in this repository.
- Styling is tokens-only (`--sc-*`); no Tailwind; no raw hex/px in component CSS.
- The database schema changes **only** via Flyway migrations; JPA `ddl-auto=validate`.
- The API contract ([packages/api-client/spec/openapi.json](packages/api-client/spec/openapi.json)) is the boundary of record; frontend types are generated, never hand-duplicated.
- Secrets never enter Git (gitleaks-scanned); `.env.example` documents every variable.
- Architecture decisions live in [docs/adr](docs/adr/README.md); deviations require a new ADR.
