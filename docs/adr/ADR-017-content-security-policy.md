# ADR-017 — Content-Security-Policy Strategy for the Next.js Frontend

**Status:** Accepted (interim policy) — 2026-07-08 · Remediation milestone bound to Phase-4 public-web design
**Debt reference:** SEC-DEBT-1 (validation log)

## Context

Sprint-1 validation produced a hard, reproducible finding: with a production CSP of `script-src 'self'`, every page rendered correctly but **no page ever hydrated** — forms were completely inert, with no console error visible to casual testing. The mechanism, verified experimentally on the running production build:

1. **Rendering mode:** public and register routes are **statically prerendered (SSG)** at build time; auth/buyer/BFF routes are dynamic. This mix is deliberate (performance budgets, CDN cacheability of public pages).
2. **Scripts requiring inline execution:** Next.js App Router embeds (a) the hydration bootstrap and (b) React Flight payload segments as **inline `<script>` tags** (`self.__next_f.push(...)`) in every HTML document. `script-src 'self'` blocks all of them.
3. **Nonce feasibility:** nonces were implemented and tested via middleware (`x-nonce` + per-request CSP header). Result: **infeasible for SSG output** — a per-request nonce cannot be injected into HTML frozen at build time, and `'strict-dynamic'` (required for the nonce pattern) additionally **ignores `'self'`**, so even the external chunk loads get blocked on static pages. Nonces work only with **fully dynamic rendering**, which would forfeit SSG/ISR and full-page CDN caching for the public site — rejected for now (contradicts approved performance architecture).
4. **Hash feasibility:** CSP `sha256-` hashes are computable for build-time-static inline scripts, but Next.js does not emit per-script hash manifests today; Flight-payload scripts vary per page/build, so hash maintenance would be manual and fragile without framework support. Revisit as Next.js CSP support evolves.
5. **CDN/performance impact of the alternatives:** dynamic-render-everything (nonce path) moves every public page to origin-rendered responses — measurable LCP/TTFB regression and loss of the static-page CWV headroom the design system budgets for.

## Decision (interim, explicitly time-bound)

Production CSP ships as: `default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'` — i.e. **`'unsafe-inline'` for scripts only, every other directive strict.**

This is **not the permanent policy**. Remediation milestone: the Phase-4 public-website design must select the target end-state and implement it before public launch.

## Compensating controls (current)

- No user-generated HTML is rendered anywhere in the current surface (all content is first-party; React escapes interpolated text by default).
- `object-src 'none'`, `base-uri 'self'`, `frame-ancestors 'none'`, `form-action 'self'` close the classic injection amplifiers.
- BFF architecture: browser holds no API tokens (JWE HttpOnly cookie), so script injection cannot exfiltrate bearer tokens.
- Auth forms carry `method="post"` (pre-hydration native submits can never leak fields into URLs).
- Regression tests: the E2E smoke suite exercises real hydration on every CI run — a CSP change that silently breaks hydration fails CI immediately.

## Alternatives considered

| Option | Verdict |
|---|---|
| `script-src 'self'` only | **Rejected — proven broken:** renders but never hydrates (silent, severe) |
| Nonce + `'strict-dynamic'` via middleware | Rejected for now: incompatible with SSG (validated experimentally); acceptable only with fully-dynamic rendering |
| Hash-based (`sha256-…`) | Deferred: no framework-emitted hash manifest; manual maintenance fragile; re-evaluate with Next.js CSP tooling |
| Fully dynamic rendering to enable nonces | Rejected: forfeits SSG/CDN performance architecture for a header improvement; may be selectively applied to auth-only routes later (hybrid) |

## Amendment (Phase 7, 2026-07-09) — `connect-src` includes the object-storage origin

Evidence upload uses presigned **direct-to-storage** PUTs (ADR-023): the browser connects to the S3/R2/MinIO origin, not to `self`. `connect-src 'self'` blocks that with a CSP violation (verified: the browser reports `csp` on the evidence PUT). `connect-src` is therefore broadened to `'self' <storage-origin>`, where the storage origin comes from `NEXT_PUBLIC_STORAGE_ORIGIN` (per environment; empty ⇒ no broadening). This is the **only** broadening, it is **documented and justified** (the direct-upload architecture requires it), and no other directive changes. The storage bucket must also carry a CORS policy allowing PUT from the web origin (see [evidence-storage-architecture.md](../security/evidence-storage-architecture.md)). `script-src`/`object-src`/`base-uri`/`form-action`/`frame-ancestors` remain strict; SEC-DEBT-1 (`script-src 'unsafe-inline'`) is unchanged.

## Remediation path (bound milestone)

Phase-4 public-web design must decide between: (a) **hybrid** — dynamic rendering + nonce CSP on authenticated/auth routes, hardened-static policy on public pages; (b) hash-manifest generation if framework support matures; (c) full dynamic + nonce if CWV budgets prove tolerant. Until then `'unsafe-inline'` is tracked as open security debt (SEC-DEBT-1) in every security review — it must not be silently normalized.

## Risks

Residual XSS-amplification risk if a template-injection defect is ever introduced; mitigated by the compensating controls above, no-UGC surface, dependency scanning, and review gates. Accepted for the current pre-launch, no-public-traffic stage.
