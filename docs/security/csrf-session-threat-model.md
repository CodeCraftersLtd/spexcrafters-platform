# CSRF & Session Threat Model — Phase 6

**Date:** 2026-07-09 · **Scope:** the request architecture as actually implemented after Phase 5 (merge `e469833`). Binding input to ADR-018.

## 1. Request architecture & trust boundaries

```
 Browser (untrusted)
   │  sc_session cookie (JWE, HttpOnly, SameSite=Lax)  +  sc_csrf cookie (JS-readable, Phase 6)
   ▼
 Next.js frontend  ────────────────  TRUST BOUNDARY A (cookie-authenticated surface)
   ├─ Server Components (reads): unseal session → bearer call to API
   └─ BFF route handlers (/api/**): unseal session → refreshIfNeeded → bearer call
        ▼
 Spring Boot API (/api/v1/**) ─────  TRUST BOUNDARY B (bearer-only surface)
        ▼
 PostgreSQL (tokens stored hash-only)
```

- **Boundary B is CSRF-immune by construction**: the API accepts only `Authorization: Bearer` (no cookie authentication, no CORS allowance for browser calls); a cross-site attacker cannot attach a bearer token. CSRF work therefore concentrates on **Boundary A** — every Next.js endpoint that reads `sc_session`.
- Session state: JWE (A256GCM) cookie sealed with `SESSION_SECRET`; contains access token (10 min), refresh token (opaque, rotating), user summary. Browser JavaScript can never read it (HttpOnly) — and must never be able to.

## 2. Request classification (implemented surface)

| Endpoint (BFF unless noted) | Auth’d? | State-changing? | Class |
|---|---|---|---|
| `GET` pages, `GET /api/auth/session` | either | no | Safe — no CSRF defense needed |
| `POST /api/auth/register`, `/verify-email`, `/resend-verification` | no | yes (account state) | **Unauthenticated state-changing** |
| `POST /api/auth/login` | no | yes — **session-establishing** | Login-CSRF surface |
| (internal) session refresh inside BFF handlers | yes | yes — **session-renewing** | Not directly invokable; rides authenticated requests |
| `POST /api/auth/logout` | yes | yes — **session-destroying** | Logout-CSRF surface |
| `POST /api/orgs`, `PATCH /api/orgs/[id]`, `POST …/invitations`, `POST …/invitations/[iid]/revoke`, `DELETE …/members/[mid]`, `PUT …/members/[mid]/role`, `POST /api/invitations/accept` | yes | yes | **Authenticated mutations** — primary CSRF surface |
| Future: supplier/catalog mutations, file uploads (presigned → storage), multipart, webhooks | — | yes | §6 forward rules |

## 3. CSRF exposure analysis

| Vector | Exposure | Verdict |
|---|---|---|
| **Authenticated mutations** | `sc_session` is SameSite=Lax: cross-site `POST` from a top-level form would not send it in modern browsers — but Lax is a compatibility default, not a guarantee (older browsers, subdomain takeover on a future shared parent domain, Lax-allowing navigations). | **Protect with an explicit token** (primary mechanism) |
| **Login CSRF** (attacker logs the victim into an attacker-controlled account, then harvests activity) | Login sets a *new* session; no pre-existing session token exists to bind. A cross-site HTML form cannot send `Content-Type: application/json`; a cross-site `fetch` with JSON triggers a CORS preflight the BFF never approves. Residual: non-JSON form posts. | **Protect via strict JSON-content-type + Origin/Fetch-Metadata checks** on all `/api/auth/*` state-changers (no token — none can exist pre-session). Same for register/verify/resend. |
| **Logout CSRF** (forced logout = annoyance/DoS, can be chained) | POST-only today, but unprotected. | **Protect like any authenticated mutation** (token + origin). Never GET. |
| **Session refresh** | Not an endpoint — refresh happens server-side inside authenticated handlers; a cross-site request that fails CSRF never reaches refresh. Backend refresh is bearer-only. | Covered transitively |
| **Invitation acceptance** | Authenticated mutation (attacker could force-accept an invitation *addressed to the victim* — a real, if narrow, state change). | **Token-protected** like other mutations |
| **Email verification** | Unauthenticated; idempotent single-use token whose possession *is* the authorization; "attack" = completing the victim's own verification — harmless. | Origin/content-type checks only (rides the `/api/auth/*` policy) |
| **Future uploads/multipart** | Multipart forms are the classic CSRF carrier (no preflight). | Forward rule: multipart mutations MUST require the CSRF header (which forces a preflighted fetch) — never cookie-auth’d “simple” multipart |
| **Future webhooks** | Server-to-server on Boundary B with signature verification; no cookies. | Out of CSRF scope by design; must never be added under Boundary A |

## 4. Non-negotiables carried into the design

Authentication credentials stay invisible to JavaScript (HttpOnly JWE only; nothing in localStorage/sessionStorage/IndexedDB/readable cookies). The **CSRF token is not an authentication credential** — possession alone authorizes nothing without the HttpOnly session cookie; it may therefore be JS-readable (cookie), which is precisely what lets same-origin JS attach it while cross-site attackers (absent XSS) cannot read it. No CSRF material in localStorage (cookie + per-request header only).

## 5. Chosen defenses (decision in ADR-018)

Primary: **session-bound synchronizer token** — canonical value sealed *inside* the JWE session; a JS-readable `sc_csrf` cookie carries the same value to the client; mutations must echo it in `X-CSRF-Token`; the BFF compares header vs sealed value in constant time. Defense in depth (single guard, all layers): Origin allow-list check, `Sec-Fetch-Site` check, JSON-only content type for BFF mutations, SameSite=Lax host-only Secure cookies. No single header is ever the sole defense.
