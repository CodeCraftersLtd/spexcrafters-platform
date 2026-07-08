# Phase 5 — Organizations Vertical Slice: Completion Report

**Date:** 2026-07-09 · **Branch:** `feature/organizations` · **PR:** [#1](https://github.com/CodeCraftersLtd/spexcrafters-platform/pull/1)
**Baseline:** `sprint-1-ci-validated` (`747896f`) · **PR HEAD:** `7cc1833` — all six required checks green (`quality`, `verify` incl. full Testcontainers suite, `client-generation`, `smoke`, `secrets-scan`, `dependency-review`; `container-scan` runs on push-to-main). **Merge commit + post-merge run IDs stamped in the final turn message.**

## Executive summary

The organizations bounded context — the capability required by the original Sprint-1 approval gate that shipped as a stub — is implemented as an independent, PR-gated vertical slice: contract-first OpenAPI + typed capabilities, backend domain/application authorization enforced at the application boundary (not controllers), application-level logical multi-tenancy with existence-concealment, hash-only email-bound invitations, a `V2` Flyway migration, capability-gated frontend, and a two-user Playwright isolation journey. All authorization invariants the gate demanded — cross-tenant isolation (both directions), privilege-escalation prevention, and last-owner protection — are proven by Testcontainers integration tests plus the E2E journey through public boundaries only. Merged through PR with all mandatory checks green; branch protection prevents direct pushes to `main`.

## Branch protection (precondition)

Enabled on `main` via the GitHub API with the **actual** check contexts read from the green baseline commit: `quality`, `verify`, `client-generation`, `smoke`, `secrets-scan`, `dependency-review`. Strict up-to-date required; PRs required; conversation resolution required; force-pushes and deletions blocked; `enforce_admins: true`. Dependency graph enabled so `dependency-review` runs (first PR run failed until enabled — remediated).

## Domain model

| Concept | Notes |
|---|---|
| Organization | id (UUIDv7), name, type {BUYER,SUPPLIER,HYBRID}, country (nullable CHAR(2)), status {ACTIVE,SUSPENDED}, `@Version` |
| OrganizationMembership | organizationId, userId, role, status {ACTIVE,REMOVED}, joinedAt/removedAt; state-transition methods enforce invariants |
| OrganizationRole | OWNER(3) > ADMIN(2) > MEMBER(1); `isAtLeast`/`higherThan` |
| Capability | typed enum, wire names via `@JsonValue`; `forRole(role)` = the matrix |
| OrganizationInvitation | email (citext), role {ADMIN,MEMBER}, tokenHash (SHA-256), status {PENDING,ACCEPTED,REVOKED,EXPIRED}, invitedBy, expiresAt, acceptedAt/By |
| InvitationToken | 32-byte SecureRandom base64url; only the hash is persisted |

## Capability & role matrix

See [organizations-capability-model.md](../architecture/organizations-capability-model.md) (binding). OWNER: all. ADMIN: read/update/members.read + invite/remove MEMBER-rank only. MEMBER: read + members.read. Rank rules prevent acting on ≥ own rank (OWNER excepted); `roles.manage` is OWNER-only; self role-change forbidden.

## Membership lifecycle

Atomic org+creator-OWNER creation; removal under rank rules; self-removal (leave) allowed except last OWNER; **last-owner invariant** enforced by a `PESSIMISTIC_WRITE` lock on the organization row around every owner-affecting mutation (remove/demote/leave) then an ACTIVE-OWNER count check → 409 `last-owner`; ownership transition = promote a second OWNER first; re-join after removal creates a new membership row (partial unique index on ACTIVE only).

## Invitations

Create (capability + rank; duplicate ACTIVE membership / existing PENDING → 409; OWNER role rejected → 422); email carries the raw token once (never logged); accept requires an authenticated user whose account email matches (else 403 `invitation-identity-mismatch`, no org leak); single-use; expiry 7d (lazy PENDING→EXPIRED); replay/consumed/revoked/expired → 410; revoke idempotent.

## Multi-tenancy & IDOR

Every org-owned row carries `organization_id`; module-external refs are typed IDs; client-supplied org IDs are never trusted — each request resolves user → org → ACTIVE membership → role → capabilities in the `OrganizationAccess` application policy. Non-members receive **404** (existence concealment) for all six org endpoint families; members lacking a capability receive **403**. Both are audited as `authorization.denied`.

## API endpoints (9)

`GET /me/organizations` · `POST /organizations` · `GET|PATCH /organizations/{id}` · `GET /organizations/{id}/members` · `DELETE /organizations/{id}/members/{mid}` · `PUT /organizations/{id}/members/{mid}/role` · `GET|POST /organizations/{id}/invitations` · `POST /organizations/{id}/invitations/{iid}/revoke` · `POST /invitations/accept`. Contract committed; TS client regenerated deterministically; no hand-written models.

## Frontend routes/components

`/organizations` (list + empty state), `/organizations/new` (RHF+Zod), `/organizations/[id]` (capability-gated workspace: members with rank-aware remove/leave + OWNER-only role select, invitations with capability-gated invite/revoke, update-gated profile form, 404 concealment state), `/invitations/accept` (410/403/409 states, returnTo-preserving login). 7 BFF mutation proxies. Middleware guards `organizations|invitations`. MERIDIAN tokens; generated client.

## Audit events

`organization.created|updated`, `organization.invitation.created|revoked|accepted`, `organization.member.removed|role_changed`, `authorization.denied` — actor, org, target, timestamp, correlation ID; no tokens/credentials. (Denial encodes checked capability in `target_id` — audit_log lacks a detail column; noted debt.)

## Test evidence (to be stamped with final CI run IDs on merge)

- Backend unit + domain: 27 new (Capability matrix, rank rules, entity invariants, token). ArchUnit: 9 rules (2 new — organizations↔identity boundary). 
- Testcontainers (`verify`): OrganizationFlow, **TenancyIsolation (both directions, all endpoint families, unknown-id indistinguishability, denial audits)**, MembershipLifecycle (**escalation, last-owner + ownership transition, invitation replay/mismatch/duplicate**), Flyway V1→V2 stepwise + from-zero.
- Frontend: 47 vitest (25 new: org schemas, accept-error mapping, capability-gating component). 
- E2E (`smoke`): two-user journey — create → concealment both directions → Mailpit invite/accept → MEMBER capability enforcement → logout guard. Verified locally against the real backend; green in CI.

## New technical debt

| ID | Item |
|---|---|
| TD-9 | Denial audit encodes capability in `target_id`; add an `audit_log.detail`/metadata column |
| TD-10 | Invitation identity-mismatch (403) not audited as `authorization.denied` (design choice; revisit in security review) |
| TD-11 | No `organization.delete` / clear-country operation in this slice |

## Known limitations & deviations

Self role-change → 409 `invalid-role-change` (doc said "forbidden"; 409 chosen). Revoking an EXPIRED invitation → 410. Invitation email dispatched inside the transaction via `@Async` (mirrors VerificationMailer). No org deletion.

## Readiness for Phase 6 (CSRF/session hardening)

Foundation is ready: BFF holds tokens; all mutations flow through BFF proxies (single choke point for CSRF tokens); session util centralizes cookie handling. ADR-018 (CSRF) is the first Phase-6 task. No blocker discovered.
