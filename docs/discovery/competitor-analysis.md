# A + B — Competitor Analysis & Feature Inventory

**Project:** SpexCrafters — Global B2B Optical Marketplace and Sourcing Platform
**Reference competitor:** www.opticleague.com (desktop), m.opticleague.com (mobile), api.opticleague.com
**Date:** 2026-07-08
**Sources:** Direct HTTP inspection performed in [CURRENT_SITE_AUDIT.md](../../CURRENT_SITE_AUDIT.md) (same date), plus a rendered-fetch attempt confirming the competitor ships an empty client-rendered shell to crawlers. Everything below is derived from publicly observable behavior. Items that could not be directly observed are explicitly labeled **[ASSUMPTION]**.

> **Brand identity (confirmed by stakeholder, 2026-07-08).** The platform is **SpexCrafters** — the Global B2B Optical Marketplace and Sourcing Platform. **OpticLeague is strictly a competitor** used for functional and market benchmarking. SpexCrafters is not an OpticLeague redesign, rebrand, replacement, or re-platforming, and shares none of its source code, content, branding, datasets, or visual identity. The root `CURRENT_SITE_AUDIT.md` predates this clarification and carries a corresponding reframing note.

---

## A. Executive Summary

### A.1 What the competitor actually is

OpticLeague presents itself with marketplace vocabulary but is, functionally, a **single-vendor wholesale eyewear ordering portal**: one storefront, one checkout, factory-direct inventory (unit prices ≈ $1.47–$9.27, MOQs around 6 units, tiered wholesale pricing, 13% Chinese VAT on invoices), backed by a private JSON API shared with an ERP that also serves the consumer brand Lensmart (`img.erp.lensmartonline.com` appears in live product payloads). There is no supplier directory, no RFQ mechanism, no buyer–supplier messaging, and no multi-party trust infrastructure. "Factories" exist as internal identifiers (`factory_goods_id`, per-factory image paths) but are never surfaced as public entities.

### A.2 Apparent business model

- **Revenue:** direct wholesale margin on factory-sourced eyewear (frames, sunglasses, reading/sports/functional glasses, lenses, contact lenses, tools/accessories), plus a paid logo-engraving customization service (≈ $0.26/unit) and volume incentives via member levels ("Members Only: More Purchases, Bigger Discounts!").
- **Demand generation:** [ASSUMPTION] predominantly paid/social and offline channels (GA4 + Meta Pixel + Microsoft Clarity installed; cross-promotion of the affiliated World Optics Fair), because organic search is structurally impossible — the site is a client-rendered SPA whose every URL serves an empty document titled "Welcome to OpticLeague", with a one-URL sitemap.
- **Fulfillment:** cart → order → online payment or bank transfer → logistics tracking → refund flow; freight multiplier 1.5 in site config.

### A.3 Target users

Small-to-mid optical retailers, e-commerce eyewear resellers, and independent opticians buying finished eyewear at low MOQs — i.e., the *shallow* end of B2B optical procurement. Not served: laboratories, chains, and procurement teams that need supplier comparison, custom manufacturing (OEM/ODM), technical lens sourcing by specification, machinery/equipment, or negotiated quotation workflows.

### A.4 Strengths (to respect, not dismiss)

1. **A genuinely strong optical product data model** — frame measurements (lens/bridge/temple/frame width, lens height), prescription parameters (SPH/CYL) on lens SKUs, tiered pricing, MOQ, stock, color SKUs, logo-customization flags. This is real domain specialization most generic marketplaces lack.
2. **A complete, working transactional loop** — order lifecycle including bank transfer (the dominant B2B payment reality), refunds, logistics tracking, order PDF export, re-purchase.
3. **Trust primitives in embryo** — buyer company verification with document upload, member levels.
4. **Factory-direct price positioning** that is hard to beat on cost for finished commodity eyewear.
5. **An editorial surface** (News hub: Brand/Industry/Exhibition) and an affiliated trade-fair property (opticsfair.com) — raw material for an industry-audience play.

### A.5 Weaknesses

1. **Existentially broken SEO:** CSR-only shell, identical metadata on every route, one-URL sitemap, UA-sniffing 302 to a duplicate `m.` host with no canonical/alternate pairing, zero structured data, zero OpenGraph.
2. **No marketplace layer:** no suppliers as entities, no RFQ, no quotations, no messaging — a buyer wanting 5,000 custom units gets the same UI as one buying 6.
3. **No brand or design system:** default Element Plus/Vant components, single-weight Inter, unharmonized palette, a loading animation as the first brand impression.
4. **Hard accessibility failures:** pinch-zoom disabled, no-JS = no content, vw-based font sizing, no focus/skip-link/reduced-motion strategy.
5. **Duplicated architecture:** two separate Vue SPAs (desktop + mobile) implementing every feature twice.
6. **Trust deficit:** no legal identity/imprint, no certifications narrative, internal ERP identifiers and a second company's domain leaking through the public API, a production PDF literally named "Draft".
7. **Compliance exposure:** three trackers fire unconditionally with no consent management (GDPR/ePrivacy).
8. **Thin discovery:** search facets return empty in practice; the rich lens parameters are not searchable.
9. **Fake i18n:** a client-side machine-translation Web Worker rewriting the DOM — invisible to crawlers, uncontrolled quality, breaks screen-reader `lang` attribution.

### A.6 Opportunity

The global optical supply chain (lenses, frames, coatings, machinery, consumables) still transacts largely through trade fairs, WhatsApp/WeChat, email spreadsheets, and horizontal platforms (Alibaba/Global Sources) that cannot express optical technical specifications. **No incumbent in this niche combines: (a) a specification-grade optical product model, (b) a verified multi-supplier directory, (c) an RFQ/quotation workflow, and (d) a search-indexable, premium, international web presence.** OpticLeague proves demand exists at the commodity end and simultaneously demonstrates that the niche's table stakes (SEO, trust, supplier discovery) are unclaimed. SpexCrafters's opening is to become the **specification-first sourcing layer for the optical industry** — the place where a lab sources 1.67 MR-7 blue-cut lenses by refractive index and coating stack, not by scrolling photos.

---

## B. Competitor Feature Inventory

Classification: **RETAIN** (adopt concept as-is) · **IMPROVE** (adopt but materially upgrade) · **REPLACE** (need exists; different mechanism) · **REMOVE** (do not carry over) · **ADD** (absent in competitor; SpexCrafters differentiator).

### B.1 Discovery & catalog

| # | Feature (observed) | Classification | SpexCrafters disposition |
|---|---|---|---|
| B1.1 | 8-group optical taxonomy (Eyeglasses, Sunglasses, Reading, Sports, Functional, Lenses, Contact Lenses, Tools & Accessories) | **IMPROVE** | Re-map into a marketplace taxonomy with three top verticals — Lenses, Frames & Eyewear, Machinery & Equipment (+ Accessories/Packaging) — keeping the observed sub-categories as facets/child categories |
| B1.2 | Product detail: frame measurements, color SKUs, weight | **IMPROVE** | First-class attribute schema per category; spec table UI; unit-aware values |
| B1.3 | Prescription parameters (SPH/CYL) on lens SKUs | **IMPROVE** | Full Rx-range model (sphere/cylinder/addition ranges, prism support) as searchable attributes |
| B1.4 | Tiered wholesale pricing (`price_layer`) | **IMPROVE** | Price-tier model with visibility rules (public / login-gated / verified-gated) — policy is Open Question OQ-7 |
| B1.5 | MOQ per product (`start_num`) | **RETAIN** | Core marketplace field, surfaced on cards and filters |
| B1.6 | Stock / frozen stock | **IMPROVE** | Availability indicator; exact stock optional per supplier policy |
| B1.7 | Keyword search + category browse | **REPLACE** | Faceted search on PostgreSQL FTS + pg_trgm behind a `SearchService` abstraction (autocomplete, typo tolerance, saved searches, URL-shareable filter state) |
| B1.8 | Filter API returning empty facets in practice | **REPLACE** | Real facet counts computed from the attribute registry |
| B1.9 | "Latest goods" / "random goods" merchandising rails | **IMPROVE** | Curated + rule-based rails (trending, new suppliers, featured) managed via admin CMS |
| B1.10 | Product names as opaque SKU codes, empty descriptions | **REMOVE** | Enforce human-readable names + structured descriptions at product-publication validation |

### B.2 Commerce & transactions

| # | Feature | Classification | Disposition |
|---|---|---|---|
| B2.1 | Cart → order creation → payment (online + bank transfer) | **REPLACE (deferred)** | SpexCrafters v1 is RFQ/quotation-led, not cart-led. Direct ordering/checkout is a later phase once marketplace liquidity exists. Bank-transfer instructions concept retained for that phase. SpexCrafters is a new, independent build — the competitor's live order flow imposes no continuity requirement. |
| B2.2 | Order statuses, logistics tracking, refunds, re-purchase | **REPLACE (deferred)** | Same as B2.1 — modeled in roadmap Phase 11+ ("transactions") |
| B2.3 | Order PDF export | **RETAIN (as quotation PDF)** | Quotation/RFQ PDF export in v1 |
| B2.4 | Freight multiplier, invoice tax handling | **REPLACE (deferred)** | Belongs to future order module; quotation carries Incoterms + payment terms instead |
| B2.5 | Logo engraving / customization service | **IMPROVE** | Generalized customization options on products: OEM / ODM / private-label capability flags + per-option lead-time/MOQ effects |
| B2.6 | Member levels / loyalty ("bigger discounts") | **REPLACE** | Supplier tiering + buyer verification status instead of consumer-style loyalty; per-card loyalty filler copy removed |

### B.3 Accounts & trust

| # | Feature | Classification | Disposition |
|---|---|---|---|
| B3.1 | Email/password registration, email verification codes, password reset | **IMPROVE** | Standards-based auth (OIDC/OAuth 2.1 + PKCE, BFF pattern), MFA/TOTP, passkey-ready |
| B3.2 | Buyer company verification (document upload) | **IMPROVE** | Full verification program for **both** buyers and suppliers: document review workflow, verified badge, admin queue, audit trail, re-verification cadence |
| B3.3 | Profile, address book | **RETAIN** | Organization-scoped profiles; addresses used for RFQ destination |
| B3.4 | Favorites / wishlist | **IMPROVE** | Favorites for products **and** suppliers + saved searches with alerting |
| B3.5 | Single-user accounts | **REPLACE** | Organizations with multiple employees, role assignments, multi-org membership |
| B3.6 | No supplier-facing accounts at all | **ADD** | Complete supplier portal: onboarding, company profile, catalog management, RFQ inbox, quotations, team, analytics |

### B.4 Marketplace mechanics (competitor: absent)

| # | Feature | Classification | Disposition |
|---|---|---|---|
| B4.1 | Supplier directory & public supplier profiles | **ADD** | Verification-first supplier pages: capabilities, certifications, export markets, factory evidence, catalog |
| B4.2 | RFQ marketplace (public + private invitations) | **ADD** | Core platform capability — see user journeys B4–B6, S4–S5 |
| B4.3 | Quotation submission, revision, withdrawal, comparison, award | **ADD** | Structured quotations: price, currency, MOQ, lead time, Incoterms, payment terms, validity, documents |
| B4.4 | Buyer–supplier messaging | **ADD** | Database-backed threads (RFQ-linked, product-linked), attachments, read state, moderation; polling/SSE first, no WebSockets in v1 |
| B4.5 | Notifications (in-app + email) | **ADD** | Template-driven notification module |
| B4.6 | Supplier comparison | **ADD** | Compare suppliers/products side-by-side |

### B.5 Content, SEO & marketing

| # | Feature | Classification | Disposition |
|---|---|---|---|
| B5.1 | News hub (Brand / Industry / Exhibition) | **IMPROVE** | "Insights" editorial hub + Events (trade fairs) with Article/Event schema; rights-cleared syndication only |
| B5.2 | Buyer's Guide PDF ("…Draft…" filename in production) | **REPLACE** | On-site HTML guides; PDFs as secondary downloads |
| B5.3 | FAQ | **RETAIN** | Rewritten, structured, FAQ schema |
| B5.4 | Homepage = ad-hoc banner JPGs + SKU grids | **REPLACE** | Designed homepage per deliverable K/L; admin-managed featured content |
| B5.5 | SEO infrastructure (broken: CSR shell, 1-URL sitemap, duplicate `m.` host) | **REPLACE** | SSR/SSG, per-route metadata, canonical, hreflang, segmented sitemaps, structured data, redirect manager — see [api-architecture](../architecture/api-architecture.md) and SEO strategy in roadmap |
| B5.6 | Client-side machine-translation worker | **REPLACE** | Real i18n: locale routing, translation files (en/zh-Hans/fr/de), localized metadata |
| B5.7 | Newsletter capture endpoint (`recordEmail`) | **IMPROVE** | Consent-managed newsletter with double opt-in |
| B5.8 | GA4 + Clarity + Meta Pixel firing without consent | **REPLACE** | Analytics behind a consent-management layer; first-party Core Web Vitals monitoring |
| B5.9 | World Optics Fair cross-promotion banners | **IMPROVE** | Formal Events section; partnership co-branding only if the relationship is official (OQ-5) |

### B.6 Platform & operations

| # | Feature | Classification | Disposition |
|---|---|---|---|
| B6.1 | Two separate SPAs (desktop + mobile) with UA sniffing | **REMOVE** | One responsive Next.js codebase |
| B6.2 | Unversioned `{code,msg,data}` API, internal ERP IDs leaking publicly | **REPLACE** | Versioned REST + OpenAPI 3.1, DTO response shaping, no entity/internal-ID exposure |
| B6.3 | Chunk-failure self-repair script (deploy pain) | **REPLACE** | Immutable deployments, zero-downtime deploys, proper asset versioning |
| B6.4 | No admin surface observable | **ADD** | Full administration portal (moderation, verification, taxonomy, CMS, audit logs, feature flags) |
| B6.5 | No rate limiting on public endpoints | **ADD** | Rate limiting + brute-force protection (Redis token buckets) |
| B6.6 | No legal pages beyond privacy policy | **ADD** | Terms, cookie policy, accessibility statement, imprint/legal identity |

### B.7 Inventory summary

- **RETAIN:** 4 · **IMPROVE:** 14 · **REPLACE:** 13 · **REMOVE:** 3 · **ADD:** 12
- The ADD column *is* the product: supplier entities, RFQ/quotation workflows, messaging, verification, and admin tooling constitute the marketplace layer the competitor lacks entirely. The IMPROVE column protects its one real asset — optical-domain data depth — and raises it to specification-grade.
