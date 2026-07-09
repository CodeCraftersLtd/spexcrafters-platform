# ADR-018 — CSRF Protection for the BFF Cookie-Authenticated Surface

**Status:** Accepted — 2026-07-09 (implemented in Phase 6; evidence: backend/BFF/E2E security suites)
**Inputs:** [csrf-session-threat-model.md](../security/csrf-session-threat-model.md) · ADR-006 (auth) · ADR-017 (CSP)

## Context

All browser mutations flow through Next.js BFF route handlers authenticated by the HttpOnly JWE `sc_session` cookie. SameSite=Lax was the only CSRF mitigation — explicitly insufficient per the Phase-6 gate. The Spring API is bearer-only and CSRF-immune; protection belongs at the BFF boundary, centrally.

## Decision

**Session-bound synchronizer token, delivered via a JS-readable companion cookie, validated by a single BFF guard, with layered origin defenses.**

1. **Issuance & binding:** at session creation (login) the BFF generates a 32-byte random token, seals it **inside** the JWE session payload (`csrfToken`), and sets the same value as cookie **`sc_csrf`** (Secure in prod, SameSite=Lax, Path=/, host-only, **not** HttpOnly — deliberately readable; see distinction below). The sealed copy is canonical: a token is valid only for the session it was born with.
2. **Attachment:** same-origin client code reads `sc_csrf` and sends `X-CSRF-Token` on every mutation (one shared fetch helper — no per-component logic).
3. **Validation:** a single `requireCsrf` guard wraps every state-changing BFF handler: method in {POST, PUT, PATCH, DELETE} → require (a) header present, (b) constant-time equality with the sealed session value, (c) origin layer below. Safe methods bypass. Failure → 403 `{error:{code:'csrf'}}`, structured log (never the token value).
4. **Origin layer (defense in depth, same guard):** if `Origin` is present it must equal the site origin; else if `Sec-Fetch-Site` is present it must be `same-origin`/`none`; `Referer` accepted as a last-resort corroborator only. BFF mutations additionally require `Content-Type: application/json` (multipart/forms are thereby excluded from the cookie surface — the forward rule for uploads). No header is ever the *sole* defense; the token check always runs for authenticated mutations.
5. **Unauthenticated state-changers** (`/api/auth/login|register|verify-email|resend-verification`): no session exists to bind a token, so protection = origin layer + JSON-only content type (cross-site HTML forms cannot send JSON; cross-site JSON fetch dies in CORS preflight). This is the **login-CSRF** decision: architecture cannot use a synchronizer pre-session; the layered origin checks close the vector.
6. **Lifecycle:** token lives exactly as long as the session — rotated on login (new session), preserved across access-token refresh (session identity is continuous; renewal does not re-issue), destroyed on logout (session cookie and `sc_csrf` both deleted with matching attributes). **Multi-tab:** the cookie is shared by all tabs, so concurrent tabs never diverge — no per-request token churn that would break parallel usage. **Expiry:** with the session (idle 14 d / absolute cap; see session policy).
7. **Logout** is a POST protected like any mutation (token + origin); GET logout does not exist.

**Credential vs CSRF-token distinction (explicit):** the JWE session cookie *is* the credential and stays HttpOnly forever. `sc_csrf` authorizes nothing by itself — replaying it without the HttpOnly cookie is useless; its only role is proving the request originated from a same-origin context able to read our cookies. JS-readability is therefore by design and not a credential exposure. Nothing touches localStorage.

## Alternatives considered

| Option | Verdict |
|---|---|
| Naked signed double-submit cookie (no session binding) | Rejected: vulnerable to cookie-fixation via subdomain/MITM planting; our variant keeps the canonical value inside the sealed session, which double-submit alone lacks |
| Synchronizer token with server-side store | Rejected: introduces shared state (Redis/DB) the stateless BFF doesn't need — the sealed session *is* the server-side copy; horizontal scaling stays free |
| Per-request rotating tokens | Rejected: breaks multi-tab and retries for negligible gain over per-session tokens |
| Framework CSRF (Spring Security) | Rejected: wrong boundary — the API is bearer-only; protection must live where cookies live |
| Origin/Fetch-Metadata alone | Rejected as primary (header-stripping proxies, older clients); adopted as the depth layer |
| Meta-tag token embedding | Rejected: forces dynamic rendering of every page (conflicts with SSG/ADR-017); cookie transport doesn't |

## Consequences & risks

- All mutation handlers must route through the guard — enforced by BFF tests that enumerate route files and assert guard usage (a lint-like test, not convention hope).
- An XSS defeats CSRF protection (attacker reads `sc_csrf`) — true of every CSRF scheme; XSS remains governed by CSP (ADR-017) and no-UGC posture. CSRF work required **no CSP change** (token rides cookie+header, zero inline script).
- Grace note: `GET /api/auth/session` returns no CSRF value; the cookie is the sole client transport (one mechanism, not two).

## Migration path

If the BFF is ever bypassed (native apps, third-party API consumers), those clients use pure bearer auth on Boundary B and are untouched. If SameSite=Strict becomes viable UX-wise, it layers on without design change.
