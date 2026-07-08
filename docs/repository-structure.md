# N — Repository Structure (Monorepo)

**Project:** SpexCrafters · **Date:** 2026-07-08
One Git monorepo. Frontend workspaces via **pnpm**; backend via **Maven multi-module**. Only packages with a clear ownership boundary exist (brief §28).

```
spexcrafters/
├── apps/
│   ├── web/                          # Next.js 16+ (App Router, TS strict)
│   │   ├── src/
│   │   │   ├── app/                  # [locale]/(public|auth|buyer|supplier|admin)/…
│   │   │   ├── components/           # app-specific composites (design-system consumed from packages)
│   │   │   ├── features/             # feature folders (rfq/, search/, messaging/…): client islands + hooks
│   │   │   ├── lib/                  # BFF auth, api-client wiring, i18n, seo helpers
│   │   │   └── styles/               # global.css, layers, token imports
│   │   ├── public/
│   │   ├── messages/                 # en.json, zh-Hans.json, fr.json, de.json
│   │   ├── e2e/                      # Playwright journeys
│   │   └── next.config.ts  package.json  tsconfig.json
│   │
│   └── api/                          # Spring Boot modular monolith (Java 25)
│       ├── pom.xml                   # parent
│       ├── application/              # bootable app: config, security wiring, composition root
│       ├── modules/
│       │   ├── shared-kernel/
│       │   ├── identity/  organizations/  verification/
│       │   ├── suppliers/  buyers/
│       │   ├── catalog/  products/  search/
│       │   ├── rfq/  quotations/
│       │   ├── messaging/  notifications/  favorites/
│       │   ├── media/  content/  seo/
│       │   ├── localization/  currency/
│       │   ├── analytics/  audit/  administration/
│       │   └── (each: api/ domain/ infrastructure/ web/ + src/test)
│       ├── db/migration/             # Flyway V###__module_desc.sql (single history)
│       └── architecture-tests/       # ArchUnit module-boundary suite
│
├── packages/
│   ├── design-tokens/                # JSON source → build: CSS custom props + TS constants
│   ├── ui/                           # design-system React components (CSS Modules) + Storybook
│   ├── api-client/                   # committed openapi.json + generated types/client
│   ├── config/                       # shared eslint, tsconfig, prettier, stylelint presets
│   └── shared-types/                 # cross-app TS types NOT derivable from OpenAPI (analytics events, i18n keys)
│
├── infrastructure/
│   ├── docker/                       # Dockerfiles (web, api), compose.yaml (pg, redis, minio, mailpit, otel-collector)
│   └── terraform/                    # envs/{dev,staging,prod}, modules/{network,db,storage,redis,dns,cdn}
│
├── docs/
│   ├── discovery/                    # A, B, C
│   ├── architecture/                 # D, E, F, G, H, J + threat model
│   ├── database/                     # I + ERD exports
│   ├── design-system/                # K, L
│   ├── api/                          # conventions, changelogs
│   ├── adr/                          # ADR-001…NNN
│   ├── security/                     # ASVS checklist, secrets policy, prod checklist
│   ├── deployment/                   # runbooks, rollback, DR, backup/restore
│   ├── roadmap.md  repository-structure.md  sprint-01.md  dependencies.md
│
├── .github/workflows/                # ci-web.yml, ci-api.yml, contract.yml, e2e.yml, security.yml, deploy.yml
├── .env.example                      # documented, no secrets ever committed
├── pnpm-workspace.yaml  package.json  turbo?  # task runner added only if build times demand
├── CODEOWNERS  CONTRIBUTING.md  README.md
```

## Ownership boundaries

| Path | Owner | Rule |
|---|---|---|
| `packages/design-tokens`, `packages/ui` | Design-system | Apps never define raw color/size values; only tokens |
| `packages/api-client` | Backend (generated) | Never hand-edited; regenerated in CI from committed spec |
| `apps/api/modules/*` | Per-context owner | Cross-module imports only via `…api` packages (ArchUnit-enforced) |
| `apps/api/db/migration` | Backend + DBA review | Flyway-only; PR checklist for expand→contract discipline |
| `infrastructure/*` | DevOps | Terraform plans reviewed; no console changes |
| `docs/adr` | Architecture | New ADR required for any decision deviating from H |

## Conventions

- Trunk-based development: short-lived branches → PR → required checks (lint, typecheck, unit, integration, contract diff, ArchUnit, Lighthouse budget on affected routes) → squash merge.
- Conventional Commits; automated changelog; release tags per deployable (`web-vX`, `api-vX`).
- CI paths-filtering so web-only changes don't run the JVM suite and vice versa; contract job runs on either side touching the API surface.
- Secrets only via environment/secret manager; `.env.example` enumerates every variable with comments; gitleaks scan in CI.
