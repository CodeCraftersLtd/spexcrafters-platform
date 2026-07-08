# Sprint 1 — First-Build Validation & Stabilization Log

**Started:** 2026-07-08 · **Baseline:** commit `b1f2fd385de182031b8445e1706621f0bab70daf` (tag `sprint-1-pre-validation`)
**Objective:** prove the Sprint 1 implementation is reproducible, compilable, testable, and consistent with the approved architecture. No new features; no Sprint 2 work.

## Phase 1 — Baseline preservation ✅

| Item | Recorded value |
|---|---|
| Branch | `main` |
| HEAD | `b1f2fd385de182031b8445e1706621f0bab70daf` |
| Working tree | clean (`git status --short` empty) |
| History | `e7030c7` → `c94fde6` → `b1f2fd3` (no rewrites, no squash, no force push) |
| Remotes | none configured |
| Tag created | `sprint-1-pre-validation` (annotated) → `b1f2fd3` |
| `git fsck --full` | no errors |
| Tracked files | 242 |
| Backend modules | `shared-kernel`, `identity`, `audit`, `organizations` (stub), `application`, `architecture-tests` (Maven multi-module, parent `apps/api/pom.xml`, Spring Boot 3.5.6, Java 25) |
| Frontend | `apps/web` Next.js App Router (`[locale]/(public|auth|buyer)` + `api/auth/*` BFF), workspaces `packages/{design-tokens,ui,api-client,config}` |
| Flyway migrations | `apps/api/application/src/main/resources/db/migration/V1__identity_baseline.sql` (single migration; schemas `identity`, `audit`; citext) |
| OpenAPI config | committed contract `packages/api-client/spec/openapi.json`; springdoc 2.8.13 exports `/v3/api-docs` |
| CI | `.github/workflows/{ci-web,ci-api,contract,security,e2e}.yml` |
| Test inventory (backend) | unit: `UuidV7Test`, `PasswordPolicyTest`, `TokenHasherTest`, `OpaqueTokenGeneratorTest`, `LoginThrottleTest`; integration (Testcontainers): `AuthFlowIntegrationTest`, `RefreshTokenRotationIntegrationTest`, `ProblemJsonIntegrationTest`, `FlywayMigrationTest`; architecture: `ModuleArchitectureTest` |
| Test inventory (frontend) | vitest: auth schemas, session JWE roundtrip, LoginForm; ui package: Button/FormField/Alert; Playwright: `e2e/auth.smoke.spec.ts` (@smoke) |

## Phase 2 — Relocation out of OneDrive ✅

- Method: `git clone --no-hardlinks` from the OneDrive working copy (acts as origin/backup) → **`C:\dev\spexcrafters`**.
- Verified in the clone: HEAD `b1f2fd3…` · all 3 commits present · tag present · `git status` clean · `git diff HEAD` empty · 242 tracked files · `git fsck --full` clean.
- Original OneDrive repository left untouched.

## Phase 3 — Toolchain verification (in progress)

Environment at start: **only Git 2.55.0 installed.** No Java, Maven, Node, package manager, or Docker. `winget` present; outbound network open.

**Acquisition strategy:** portable, user-scope toolchains under `C:\dev\tools` (no admin rights assumed):

| Tool | Version selected | Source |
|---|---|---|
| JDK | Temurin **25.0.3+9-LTS** | Adoptium (zip) |
| Maven (bootstrap for wrapper) | **3.9.9** | Apache archive (zip) |
| Node.js | **v22.23.1** (latest v22 LTS line) | nodejs.org (zip) |
| pnpm | **9.15.0** (pinned via `packageManager`) | corepack |
| PostgreSQL | **17.6-1** (binaries zip) | EDB |
| Mailpit | latest | GitHub releases |
| Docker / Compose | **UNAVAILABLE — environment limitation** | requires admin + virtualization/WSL2; not installable in this session |

**Consequences of no Docker (recorded up front):**
- Testcontainers-based integration tests (`AuthFlowIntegrationTest`, `RefreshTokenRotationIntegrationTest`, `ProblemJsonIntegrationTest`, `FlywayMigrationTest`) cannot run on this machine → validated instead in CI (`ci-api` on ubuntu-latest has Docker) once a remote exists.
- Compose-stack validation (Phase 6) substituted with **portable PostgreSQL 17.6 + Mailpit** run as user processes; the compose file itself is validated in the CI `e2e-smoke` workflow.
- Version policy documented in this file and the root README.

Verified installed versions: Temurin **25.0.3+9-LTS** · Maven **3.9.9** · Node **v22.23.1** · corepack 0.34.6 / pnpm **9.15.0** · PostgreSQL **17.6** · Mailpit **v1.30.3** · Git 2.55.0. `.nvmrc` (`22`) added; `packageManager: pnpm@9.15.0` already pinned; Java toolchain enforced by maven-enforcer (`[25,26)`).

## Phase 4 — Maven Wrapper ✅
`mvn wrapper:wrapper -Dmaven=3.9.9` → `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` (script-only distribution type; jar fetched on demand — the `.gitignore` already excludes the jar). All local builds below used the wrapper.

## Phase 5 — Package manager & lockfile ✅
pnpm implied by workspace + `packageManager` field. `pnpm install` resolved **722 packages**; `pnpm-lock.yaml` committed. CI to switch `--no-frozen-lockfile` → `--frozen-lockfile` (done in this validation? **deferred to the CI-first-run change**, see Known limitations).

## Phase 6 — Local infrastructure ✅ (substituted)
Docker unavailable (no admin/virtualization) → portable **PostgreSQL 17.6** cluster (`initdb`, scram-sha-256, port 5432) + **Mailpit** (SMTP 1025 / API+UI 8025) run as user processes. Redis/MinIO not required by Sprint-1 runtime. Compose file itself validated only statically here; exercised by the `e2e-smoke` CI workflow when a remote exists. Teardown = `pg_ctl stop` + delete data dir; recreation verified (Phase 7).

## Phase 7 — Clean database migration ✅ (twice)
Run 1 (empty DB): Flyway `V1 — identity baseline` applied (0.365s), checksum `-704284322`, Hibernate `ddl-auto=validate` passed (mappings match schema). Schema verified: `identity.{user_account,email_verification_token,refresh_token,login_attempt}`, `audit.audit_log`; unique indexes on email/token hashes; `login_attempt(email,at)`; **token columns are hash-only** (`token_hash`, `password_hash` — no raw-token columns). DB dropped, recreated → Run 2 from zero also succeeded. No manual SQL, no Hibernate DDL.

## Phase 8 — Backend compile & test ✅ (with environment caveat)
`./mvnw verify` (integration tests excluded — Docker-gated): **BUILD SUCCESS on first compile under Java 25.** Tests: shared-kernel 4/4, identity 17/17, **ArchUnit 7/7 executed (not skipped) — ArchUnit 1.4.1 parses Java 25 class files**, application 0 (integration deferred), total 28/28 green. Springdoc 2.8.13 serves `/v3/api-docs` at runtime. Testcontainers suites (`AuthFlow`, `RefreshTokenRotation`, `ProblemJson`, `FlywayMigration`) **NOT RUN here** — require Docker; they are the CI gate. Their behaviors were instead validated live against the running app (Phase 11 evidence below), including email-boundary token retrieval and hash-only persistence.

## Phase 9 — Frontend install/build/test ✅ (after fixes F1–F6)
Final state: `pnpm -r lint` PASS (2 warnings: unused vars in a test mock) · `pnpm -r typecheck` PASS (all 6 workspaces) · unit tests: design-tokens check + ui 3 files + web 3 files (22 tests) all green · `next build` PASS (SSG for public/register routes; dynamic for auth/buyer/BFF). MERIDIAN tokens compile; auth components build. Organization workflow components: **N/A — not in Sprint-1 scope** (organizations module is a stub by design).

## Phase 10 — OpenAPI contract & client pipeline ✅
Live spec (`/v3/api-docs`, OpenAPI 3.1.0) fetched twice → **byte-identical (deterministic)**. All 7 contract operations present live. Known cosmetic divergence: committed contract puts `/api/v1` in `servers[]`; springdoc emits it in paths — semantically equivalent, normalization tracked (TD-3). Client generation (`openapi-typescript`) run twice → **byte-identical**; generated `schema.d.ts` committed; api-client typechecks. No manual duplicate models beyond the documented bootstrap `types.ts` (retirement tracked, TD-2).

## Phase 11 — End-to-end smoke ✅ (implemented scope)
Playwright (Chromium): **3/3 pass** — full journey: register → verification email captured via **Mailpit API (email delivery boundary — never the DB)** → verify → login → JWE `sc_session` cookie → protected `/en/buyer` renders user → API `/me` via BFF → logout → guard redirects to login; plus signed-out redirect and locale-negotiation specs. Live API negatives verified: anon `/me` → 401 problem+json with correlationId; refresh-token **reuse → 401 family revocation**; bad password → 401; brute-force throttle in code (unit-tested). **Scope divergence (reported, not patched): organization creation/membership/authorization steps of the mandated journey are NOT IMPLEMENTED in Sprint 1** — the organizations module is a stub per the approved sprint plan; audit events verified for the implemented actions (`identity.user.registered/login/logout`, `identity.email.verified` rows present).

### Defects found & fixed during validation (root-caused, minimal scope)
| ID | Defect | Root cause | Fix |
|---|---|---|---|
| F1 | Token build outputs missing from clone → `tokens:check` fail + web build "module not found" | root `.gitignore` `build/` pattern swallowed the committed-by-policy outputs | negation rule + committed generated outputs |
| F2 | api-client typecheck: base tsconfig unresolvable → ES5 fallback | missing `@spexcrafters/config` devDependency | added workspace devDep; added `lib: DOM` for fetch types; `RequestInit` built without explicit-undefined member |
| F3 | web lint crash | Next 16 ships native flat ESLint configs; legacy `FlatCompat('next/core-web-vitals')` fails ESLint 9 validation | import `eslint-config-next/{core-web-vitals,typescript}` directly |
| F4 | LoginForm tests: duplicate elements across tests | RTL auto-cleanup inactive without vitest globals | explicit `afterEach(cleanup)` in both web and ui setups (ui already had it) |
| F5 | `exactOptionalPropertyTypes` violations (LoginForm `returnTo`, FormField `error/hint`) | library optional props not accepting explicit `undefined` | prop types widened to `string \| undefined` |
| F6 | ui typecheck: jest-dom matchers unknown; Storybook `StoryAnnotations` errors | vitest 2 vs 3 split across workspaces; render-only stories for required-children component | converged vitest ^3 / jsdom ^26; stories given `args` |
| F7 | Production pages rendered but **never hydrated** → forms inert (smoke fail) | static CSP `script-src 'self'` blocks Next's inline hydration/Flight scripts; nonce CSP validated as incompatible with SSG output (`'strict-dynamic'` also blocks `'self'` chunks) | v1 CSP allows `'unsafe-inline'` scripts with all other directives strict — recorded as **SEC-DEBT-1** |
| F8 | Pre-hydration native form submit would place credentials in the URL (GET) | `<form>` without `method` | `method="post"` on all auth forms; smoke spec additionally waits for hydration before interacting |
| F9 | Smoke fixture password violated password policy (no digit) | fixture predated policy | policy-compliant fixture |

## Phase 12 — Security validation ✅ (findings below)
Verified: Argon2id password hashing (BouncyCastle) · SHA-256-hashed single-use expiring verification tokens (raw value provably absent from DB; only delivery channel is email) · rotating single-use refresh tokens, family revocation on reuse (live-tested) · logout revocation · JWE (A256GCM) HttpOnly SameSite=Lax session cookie held only by the BFF; browser never sees API tokens · no Set-Cookie on failed login · CORS allowlist = web origin · authorization enforced server-side (anon `/me` 401) · audit events for identity actions · problem+json responses carry correlationId, **no stack traces** · no raw credentials/tokens in logs · secrets only via env (`.env.example` documented; gitleaks in CI).

**Findings by severity:**
- **Moderate / SEC-DEBT-1:** CSP `script-src 'unsafe-inline'` (see F7). Mitigations in place: no user-generated HTML rendered anywhere in Sprint 1; all other CSP directives strict. Upgrade path: nonce CSP with dynamic rendering, or hash-emission, decided in Phase-4 (public web) design.
- **Moderate (dependency):** `postcss@8.4.31` advisory GHSA-qx2v-qp2m-jg93 — transitive, pinned by Next 16.2.10 itself; no local remediation; tracked for Next patch uptake.
- **Low:** recipient email addresses logged at INFO by `VerificationMailer` (PII in logs) — acceptable at dev stage; add masking before production (TD-4).
- **Low:** CSRF on BFF relies on SameSite=Lax + JSON content type; explicit double-submit token planned with the session hardening pass (TD-5).
- **Not claimed:** production security certification. No pen test performed.

## Phase 13 — CI first run ⛔ EXTERNAL BLOCKER
No Git remote is configured and no GitHub credentials exist in this environment (`git remote -v` empty). CI workflows are authored and lint-clean but **have never executed**. Required follow-up: create the GitHub repository, push `main` + tags, confirm all five workflows green — this is also where Testcontainers integration tests and the Compose stack get their first execution.

## Phase 14 — Reproducibility ✅ (local-machine equivalent)
Fresh `git clone` of the validated commit → `pnpm install` (lockfile-driven) → `tokens:build` → `-r typecheck`/`-r test` → web production build → `./mvnw compile` — all green following only repository documentation. Undocumented steps found and fixed in docs: JVM TEMP-path constraint (below), portable-toolchain PATH notes. **Caveat:** same machine (shared pnpm store/Maven cache speeds it up but doesn't change results); a true clean-machine run happens with CI (Phase 13).

### Environment-specific constraint (documented in README)
JDK 25 on Windows creates NIO selector wakeup pipes as Unix-domain sockets under `%TEMP%`; a TEMP path longer than the 108-byte `sun_path` limit makes Tomcat fail at startup (`Invalid argument: connect`). This session's harness TEMP exceeded it; fix is a short TEMP (e.g. `C:\dev\tmp`) for the JVM process. Normal developer machines are unaffected but the symptom is documented in the API README troubleshooting section.

## Toolchain record (final)
| Tool | Version | | Tool | Version |
|---|---|---|---|---|
| JDK | Temurin 25.0.3+9-LTS | | pnpm | 9.15.0 (corepack 0.34.6) |
| Maven | 3.9.9 (wrapper-pinned) | | PostgreSQL | 17.6 |
| Node.js | v22.23.1 (`.nvmrc` = 22) | | Mailpit | v1.30.3 |
| Git | 2.55.0.windows.2 | | Docker | unavailable (env limitation) |

## Technical debt register
| ID | Item | Priority |
|---|---|---|
| TD-1 | CI `--no-frozen-lockfile` → `--frozen-lockfile` now that the lockfile is committed | High (with first push) |
| TD-2 | Retire `api-client/src/types.ts` bootstrap in favor of generated-schema re-exports | Medium |
| TD-3 | Contract job: normalize `servers[]` base vs path prefix when diffing committed spec against springdoc output | Medium |
| TD-4 | Mask email addresses (PII) in mailer logs before production | Medium |
| TD-5 | Explicit CSRF double-submit token on BFF mutations | Medium |
| TD-6 | `middleware.ts` → `proxy` file convention (Next 16 deprecation warning) | Low |
| TD-7 | `next start` warns with `output: standalone`; containers use the standalone server (Dockerfile already correct), document local dev via `next dev` | Low |
| TD-8 | 2 lint warnings (unused vars in LoginForm test mock) | Low |
| SEC-DEBT-1 | CSP inline-script allowance (see Phase 12) | High (Phase 4 design input) |

## Status: IMPLEMENTED vs VALIDATED vs PLANNED vs DEFERRED
- **IMPLEMENTED + VALIDATED (this log):** monorepo toolchain; migrations from zero (×2); backend compile + 28 unit/arch tests; identity vertical slice live (register/verify/login/refresh-rotation/logout/me + negatives); frontend lint/typecheck/tests/build; tokens pipeline; OpenAPI determinism + client generation; 3/3 E2E smoke; audit events; hash-only token persistence.
- **IMPLEMENTED, NOT YET VALIDATED:** Testcontainers integration suites, Compose stack, all five CI workflows, container images (need Docker/remote — Phase 13).
- **PLANNED (Sprint 2+):** organizations module (creation/membership/roles), MFA/TOTP, Spring Authorization Server (ADR-006 phase 2), org-scoped authorization isolation testing.
- **DEFERRED:** production security certification; load testing; clean-machine reproducibility beyond CI.
