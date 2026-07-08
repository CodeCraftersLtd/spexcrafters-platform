# M — Implementation Roadmap

**Project:** SpexCrafters · **Date:** 2026-07-08
Phases follow the master brief §36. Milestones assume one cross-functional team; durations are relative sizing (S/M/L), not calendar commitments.

## M.0 Global Definition of Done (applies to every feature)

- Backend: unit + Testcontainers integration tests green; ArchUnit module rules pass; endpoint documented in OpenAPI; permissions declared; audit logging for sensitive actions; no exposed entities.
- Frontend: Server-Component-first; states designed (empty/loading/error/success); WCAG 2.2 AA checks (axe clean + keyboard pass); i18n keys (no hardcoded strings); metadata where public.
- Contract: regenerated TS client compiles; breaking-change check green.
- Quality gates: lint, typecheck (strict), security scans green; Core Web Vitals budget respected on affected public routes (Lighthouse CI); migration validated empty + from-latest.
- Observability: new failure modes logged with correlation IDs; metrics for new critical paths.
- Docs: ADR if an architectural decision was made; changelog entry.

## M.1 Phases, milestones, dependencies

| Phase | Milestone (exit criteria) | Size | Depends on |
|---|---|---|---|
| **0 Discovery** | ✅ Done: competitor audit, feature inventory, differentiation (docs A–C) | — | — |
| **1 Product architecture** | ✅ Done in draft: sitemap, roles, journeys, modules, DB model, API, tech matrix (docs D–J) · Remaining: stakeholder sign-off + ADR-001…016 formalized + threat model workshop | S | 0 |
| **2 Design system** | Direction chosen (doc K); tokens package building (JSON→CSS vars+TS); core primitives + marketplace components in Storybook with a11y tests + visual regression; motion & a11y rules documented | M | 1 (K/L drafts exist) |
| **3 Foundation** | Monorepo + CI/CD green; Compose env (PG/Redis/MinIO/Mailpit); Spring Boot skeleton with modules, Flyway V1, actuator, OTel; Next.js skeleton with locale routing + token wiring; Auth end-to-end (register→verify→login→session BFF→MFA); OpenAPI→client pipeline; Playwright smoke (registration/login) | L | 1, 2 (tokens) |
| **4 Public website** | Homepage, category landings, product index/PDP, supplier directory/profiles, insights, events, about/contact/pricing/legal; SEO infra (metadata, sitemaps, structured data, hreflang, redirect manager); CWV budgets met; content module + seed taxonomy live | L | 3 |
| **5 Marketplace discovery** | Faceted search (FTS+trgm) with URL state; autocomplete; favorites; saved searches + alert jobs; comparison; supplier discovery facets | M | 4 |
| **6 RFQ core** | RFQ wizard (draft→publish, public/invited); RFQ board with qualification filters; quotations (submit/revise/withdraw); comparison view; award/close; messaging (threads, attachments, read state); notifications (in-app + email outbox); E2E journeys B4–B6/S4–S5 | L | 5 |
| **7 Supplier portal** | Onboarding wizard, company/profile editor with preview, verification submission, product editor driven by attribute registry, media pipeline, catalog management, RFQ inbox + analytics-lite | L | 6 (parallelizable with 8) |
| **8 Buyer portal** | Buyer dashboard, org/team management, favorites/saved-search surfaces, RFQ/quotation management UIs, notification center, settings incl. MFA & currency/locale prefs | M | 6 |
| **9 Administration** | Verification queue + decisions, moderation queues (products/RFQs/messages/reports), taxonomy manager, CMS (pages/insights/events/homepage/featured), SEO tools, templates, translation mgmt, audit-log explorer, flags/config; granular admin permissions | L | 7, 8 |
| **10 Hardening & launch** | Security test round (ASVS checklist + external pen test), load tests (search, RFQ board, messaging), a11y manual audit + screen-reader pass, SEO validation, backup **restore rehearsal**, DR runbook exercised, production-readiness review sign-off | M | 9 |
| **Post-launch (11+)** | Transactions (cart/orders/payments), passkeys & enterprise SSO, dedicated search engine if scale demands, headless-CMS integration if editorial workflow demands, ERP integration APIs, additional locales/currencies | — | liquidity signals |

## M.2 Critical-path notes & parallelization

- Longest chain: 3 → 4 → 5 → 6 → 9 → 10. Phases 7/8 run in parallel after 6's API surface stabilizes.
- Design system (2) needs only direction sign-off to start; component build overlaps Phase 3.
- Taxonomy + attribute registry seeding (lenses/frames/machinery) is content work that starts in Phase 3 and gates Phase 4 catalog pages — assign a domain owner early.
- The 17 mandated E2E journeys (brief §12) map: 3 → registration/login/org-creation; 4 → discovery/product-detail; 5 → filtering/favorites/supplier-discovery; 6 → RFQ/quote/messaging; 7 → supplier+buyer onboarding; 9 → admin moderation.

## M.3 Risk register

| Risk | Phase | Mitigation |
|---|---|---|
| Attribute-registry over-engineering stalls Phase 4 | 3–4 | Cap v1 registry features (6 data types, flat groups); registry changes are admin data, not code |
| Auth self-hosting delays foundation | 3 | Spring Authorization Server with vanilla flows only; Keycloak as tested fallback decision point at Phase 3 mid-review |
| Design ambition vs. CWV budgets | 4 | Perf budgets in CI from first page; GSAP confined to homepage hero, lazy-loaded; imagery pipeline before content load-in |
| Cold-start content (empty marketplace look) | 4–5 | Editorial/insights + category landing copy carry the public site; "founding supplier" program; no fake metrics (OQ-4) |
| Verification ops without staff | 7/9 | Tiered verification; admin queue designed for a single reviewer's workflow first |
| Scope creep toward checkout | any | Explicit non-goals in C.4; transactions live only in post-launch phase |
| OneDrive-hosted dev folder (current workspace) causing file-lock/sync issues with node_modules/build artifacts | 3 | Move the repo out of OneDrive before Phase 3 scaffolding (flagged to stakeholder) |

## M.4 Success metrics (instrumented from launch)

Activation: supplier onboarding completion rate, first-product-published time, buyer first-RFQ time · Liquidity: quotes per RFQ (target ≥3), RFQ time-to-first-quote, award rate · Engagement: saved searches per buyer, message response time/rate per supplier (future badge input) · Acquisition: indexed pages, organic sessions to PDP/supplier/category pages · Quality: CWV field data, error budget, a11y regression count (target 0).
