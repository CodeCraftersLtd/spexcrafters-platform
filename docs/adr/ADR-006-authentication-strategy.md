# ADR-006: Phased authentication strategy

## Status
Accepted — 2026-07-08

## Context
SpexCrafters needs credible B2B authentication from Sprint 1 (buyers, suppliers, admins; org-scoped access per ADR-007's future scope), but the full standards-based target — OAuth 2.1/OIDC with MFA step-up, passkeys, and enterprise SSO — is too large for the first sprint and too important to improvise. The brief (§8) mandates a standards-based, self-hosted direction; auth SaaS per-MAU pricing and lock-in were explicitly rejected in the decision matrix. The browser must never hold API tokens: the Next.js BFF (ADR-003) is the only browser-facing auth surface. Future consumers (mobile, ERP machine clients) mean sessions alone are insufficient. The decision is therefore **phased**, with Sprint 1 deliberately shaped so Phase 2 is a swap, not a rewrite.

## Decision

### Sprint 1 — email/password with BFF-held tokens
- **Credentials:** email/password only. Hashing with **Argon2id** (`password_hash` on `identity.user_account`); single-use, hashed email-verification and password-reset tokens; `login_attempt` tracking with exponential backoff + lockout (Redis rate limits per ADR/api-architecture §J.9).
- **Access tokens:** JWT, **HS256**, **10-minute** lifetime, issued by the `identity` module.
- **Refresh tokens:** opaque, **single-use, rotating**, **14-day** sliding lifetime, stored hashed in `identity.refresh_token` with device fingerprint and token-family ID. **Reuse of a consumed token revokes the entire family** (stolen-token detection).
- **Token custody:** tokens are consumed **only by the Next.js BFF**. Route handlers perform login/refresh server-side and store the token pair in an **encrypted (JWE) HttpOnly, Secure, SameSite=Lax cookie**; the browser never sees a JWT. State-changing BFF routes require the SameSite cookie + double-submit CSRF token.
- Admin scope: 2 h absolute session cap (per §J.6), tightened further in Phase 2 with step-up.

### Phase 2 — Spring Authorization Server (OAuth 2.1 / OIDC)
- Stand up **Spring Authorization Server** inside `apps/api` (extractable later), issuing tokens via **Authorization Code + PKCE**; the BFF becomes a standard confidential OIDC client performing the code exchange server-side — cookie mechanics unchanged.
- Access tokens move to **RS256 with published JWKS**; resource-server validation becomes key-based, enabling module extraction and third-party resource servers without shared secrets.
- Protocol features then unlock incrementally: **MFA/TOTP step-up** (admin scope, sensitive org actions like ownership transfer), **passkeys/WebAuthn**, **enterprise SSO** (SAML/OIDC federation for large buyer/supplier orgs), and **client-credentials** machine clients for ERP integrations.

## Alternatives considered
- **Auth SaaS (Auth0, Clerk, Cognito)** — rejected: per-MAU pricing scales against a marketplace's interest, migration off is painful (password-hash export varies, token semantics differ), and core login availability would depend on a vendor. Revisit only if self-hosted ops burden proves out.
- **Keycloak** — rejected for now: capable and standards-correct, but a heavyweight additional stateful service (its own DB, upgrade cadence, theming layer) for a small team. **Kept as the explicit fallback IdP** if Spring Authorization Server proves insufficient — the protocol boundary makes that swap contained.
- **Sessions-only (no tokens)** — rejected: simplest for the web app, but dead-ends the multi-client future (mobile apps, ERP machine clients per §G.1) and makes Phase 2 a rework instead of an upgrade.
- **Long-lived JWTs (no refresh rotation)** — rejected: revocation becomes impossible without a denylist that negates statelessness; 10-minute access + rotating single-use refresh gives short exposure windows and family-revocation on theft.

## Advantages
- Sprint 1 ships production-grade security (Argon2id, rotation, family revocation, JWE cookies) with zero new infrastructure.
- The browser-facing contract (HttpOnly cookie ↔ BFF) is identical in both phases; Phase 2 touches only the BFF's server side and the API's validation config.
- Self-hosted, standards-based: no per-MAU cost, no vendor lock-in; SSO/passkeys/MFA arrive as protocol features, not integrations.

## Disadvantages
- We own the security surface end-to-end — key management, token endpoints, brute-force defense — with no vendor to blame.
- Two phases mean transitional code: HS256 issuance in `identity` is deliberately disposable.
- HS256's shared secret couples API and issuer in Sprint 1 (acceptable while both live in one deployable; removed by RS256/JWKS in Phase 2).

## Risks
- **Self-hosted auth vulnerabilities** → OWASP ASVS checklist applied to the `identity` module; pen-test gate before Phase 2 launch; secrets in a managed KMS; `SecurityEvent`/audit trail on all auth outcomes.
- **Refresh-token theft** → single-use rotation with family revocation on reuse; device fingerprinting; suspicious-login events (`SuspiciousLogin`) feed notifications.
- **JWE cookie key compromise** → keys in KMS with rotation support; cookie payload contains tokens only (no PII); 14-day maximum blast radius plus server-side revocation.
- **Phase 2 slippage leaving transitional auth in place too long** → Phase 2 is scheduled on the roadmap with the MFA requirement for admin scope as its forcing function; Sprint 1 token issuance is confined to `identity.api` so the swap surface is one module.

## Migration path
- **Sprint 1 → Phase 2:** stand up Spring Authorization Server; BFF switches from password-grant-style internal calls to Authorization Code + PKCE redirects; API resource servers switch validation from HS256 shared secret to JWKS. User records, Argon2id hashes, and the cookie contract carry over unchanged. Refresh-token table semantics transfer to the AS's token store.
- **Phase 2 → external IdP (Keycloak or managed OIDC):** because clients speak standard OIDC, replacing the authorization server is a discovery-document and client-registration change plus user migration; resource servers only need a new issuer/JWKS URI.
- **Rollback within Sprint 1:** if JWT handling misbehaves, the BFF cookie already gives session semantics — the API can temporarily validate against the refresh-token store server-side while issues are fixed, without changing the browser contract.
