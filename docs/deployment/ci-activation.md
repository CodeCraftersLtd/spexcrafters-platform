# CI Activation Runbook — Sprint 1 Hosted Validation Gate

**Status:** ⛔ Blocked on GitHub account/credentials (external input) · **Prepared:** 2026-07-08
Everything below the "Owner actions" line is ready; the push itself requires an authenticated GitHub identity, which does not exist in the build environment.

## Pre-push checks — COMPLETED 2026-07-08

| Check | Result |
|---|---|
| Secrets in Git history | **gitleaks 8.30.1 full-history scan: 0 leaks.** 7 initial findings triaged — all intentional test fixtures / CI placeholders (base64 values decode to self-describing "not-a-real-secret" strings); allowlisted precisely in [.gitleaks.toml](../../.gitleaks.toml), never by disabling rules |
| Tracked environment files | Only `.env.example` (documented placeholders); `.env*` ignored; local tool settings ignored |
| Workflow permissions | Least-privilege `permissions: contents: read` added to all five workflows |
| CI determinism (TD-1) | `pnpm install --frozen-lockfile` in ci-web / contract / e2e (lockfile committed) |
| Wrapper-pinned Maven in CI | ci-api and e2e now use `./mvnw` (never a runner-provided Maven) |

## Owner actions (one-time)

1. Create the **private** GitHub repository (suggested: `spexcrafters/spexcrafters` or under your account).
2. Authenticate on this machine (either):
   - `gh auth login` (install GitHub CLI), or
   - a fine-grained PAT with `contents:write`, `workflows:write` on the new repo.
3. From `C:\dev\spexcrafters`:
   ```bash
   git remote add origin https://github.com/<owner>/spexcrafters.git
   git push -u origin main
   git push origin sprint-1-pre-validation sprint-1-validated
   ```
   No force pushes. No history rewrites.

## Branch protection — recommended settings for `main`

- Require a pull request before merging (≥1 approval once the team grows; admin-inclusive).
- Required status checks (exact job names): `ci-web / quality`, `ci-api / verify`, `contract / client-generation`, `e2e-smoke / smoke`, `security / secrets-scan`.
- Require branches to be up to date before merging; require linear history; block force pushes and deletions.
- (When available) require signed commits.

## First-run expectations & watch-list

The first hosted run is the **first execution ever** of: Testcontainers suites (4 classes), Docker Compose service health, both container image builds, Trivy scan, and Playwright-in-CI. Likely first-run friction, with prepared responses:

| Risk | Response if it fires |
|---|---|
| `actions/setup-java` Temurin 25 availability on the runner image | Pin a specific 25.0.x if `25` alias lags |
| Testcontainers pulls (`postgres:17`) rate-limited | Add registry mirror or authenticated pulls |
| e2e workflow `next start` vs standalone warning | Works (validated locally); if CI differs, switch to `node .next/standalone/server.js` per TD-7 |
| Playwright system deps | Already `--with-deps` |
| oasdiff job on first PR (no `main` spec yet at raw URL) | Only runs on PRs; first PR after push will resolve |

## Gate outcome

- All five workflows green → create annotated tag **`sprint-1-ci-validated`** on the exact green commit, recording: run IDs, test counts, container digests, scan results (Phase-4 checkpoint).
- Only then does organizations-module implementation begin (stakeholder gate of 2026-07-08).
- **Never** move or recreate `sprint-1-validated` / `sprint-1-pre-validation`.
