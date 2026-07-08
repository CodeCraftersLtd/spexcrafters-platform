# Session Security Policy & Cookie Matrix — Phase 6

**Date:** 2026-07-09 · **Status:** Binding; implemented alongside ADR-018.

## 1. Lifetimes

| Parameter | Value | Enforced by |
|---|---|---|
| Access token | 10 min (JWT HS256) | backend `spexcrafters.auth.access-token-ttl` |
| Refresh token | 14 d per token, **single-use, rotating** | backend; hash-only persistence |
| Idle timeout | = refresh TTL: 14 d without any renewal ends the session | rotation chain |
| **Absolute session lifetime** | **30 d per token family** — renewal denied once the family is older, regardless of activity; user must re-authenticate | backend family-age check at refresh (`spexcrafters.auth.session-absolute-ttl`) |
| CSRF token | = session lifetime (issued at login, destroyed at logout) | BFF (ADR-018) |
| Remember-me | Not supported (no longer-lived variant exists) | — |

## 2. Refresh semantics (reviewed Phase 6G)

- Opaque 32-byte tokens; SHA-256 hash persisted only; rotation on every use; family tracking via `family_id`.
- **Replay:** presenting an already-rotated token is treated as theft → entire family revoked, audited (`identity.session.replay_detected`, `identity.session.family_revoked`) — **except** within the **concurrency grace window**: reuse ≤ 15 s after rotation (`spexcrafters.auth.refresh-grace`) returns the *already-issued successor pair* (idempotent refresh, no new tokens minted, no revocation). Rationale: multi-tab browsers race benignly on refresh; the OAuth-BCP-style grace window preserves strict rotation while not logging users out for opening two tabs. After the window, reuse is always treated as hostile.
- Concurrent-refresh behavior is covered by dedicated Testcontainers tests (parallel same-token refresh → one rotation + idempotent replays inside the window; post-window reuse → family revocation).

## 3. Revocation policy

| Trigger | Behavior |
|---|---|
| Logout (current session) | Refresh family revoked server-side; `sc_session` + `sc_csrf` deleted with matching attributes; audited (`identity.user.logout`) |
| Replay detected (post-grace) | Family revoked + audited; next BFF request fails closed → login redirect |
| Password reset (existing behavior) | All refresh tokens + outstanding one-time tokens invalidated |
| Password/email change, account disable, admin revocation, logout-all-sessions | **Future hooks** — the family model supports them (revoke by user id); intentionally not implemented in Phase 6 (out of scope; tracked, not speculatively built) |
| Compromised-session response | Operational runbook: revoke all families for the user (SQL/admin action) until the admin surface lands |

## 4. Cookie matrix (Phase 6I — tested attributes)

| Cookie | Purpose | HttpOnly | Secure | SameSite | Path | Domain | Lifetime | Deleted on |
|---|---|---|---|---|---|---|---|---|
| `sc_session` | JWE session (access+refresh+user+csrf canonical) — **the credential** | **yes** | prod yes / dev no | Lax | `/` | host-only (none) | session cookie, content re-sealed on refresh | logout, auth failure |
| `sc_csrf` | CSRF token transport — **not a credential** (ADR-018) | **no — by design** | prod yes / dev no | Lax | `/` | host-only (none) | = session | logout (same attributes → consistent deletion) |

Rules: production cookies are always `Secure`; no `Domain` attribute ever (host-only prevents subdomain planting); deletion uses identical Path/attributes to guarantee removal; HTTPS enforced at the edge in production (HSTS per security headers).

## 5. Audit events (Phase 6J)

Backend (DB, via audit module): `identity.session.replay_detected`, `identity.session.family_revoked` (+ existing `identity.user.login/logout`, `identity.email.verified`, `identity.user.registered`). `session.refreshed` is **not** persisted (≈144 rows/user/day of no investigative value); refreshes are observable in structured logs. **TD-9 resolved:** `audit.audit_log.detail jsonb` added (V3 migration); organizations denial audits move the checked capability from `target_id` into `detail`; no raw tokens/credentials/PII beyond IDs in any event. BFF-side `csrf.validation_failed` is a structured web-tier log (the BFF has no DB access by design); noted as an accepted split.

**TD-10 decision:** invitation identity-mismatch **is now audited** as `organization.invitation.mismatch` with `detail = {invitationId, actorUserId}` (no emails). Threat value: detects invitation-token probing/forwarding; noise: negligible (requires an authenticated wrong-account acceptance attempt); privacy: IDs only. TD-10 closed.
