# CURRENT_SITE_AUDIT.md — Competitor Technical Audit: OpticLeague

> **Reframing note (2026-07-08, binding).** This document was originally drafted under a "website redesign" framing. That framing is **superseded**: the platform being built is **SpexCrafters**, an original and independent Global B2B Optical Marketplace and Sourcing Platform. **OpticLeague is strictly a competitor** analyzed for publicly observable functionality, weaknesses, and market opportunity. Read every observation below as competitor benchmarking evidence — not as a system to preserve, migrate, or re-platform. Statements about "preserving" flows, content dispositions (§5, §7), recommended IA (§8), migration posture (§9), and stakeholder questions (§10) reflect the obsolete framing and are superseded by the SpexCrafters deliverables in [docs/](docs/README.md). No OpticLeague source code, text, imagery, branding, or datasets are used in SpexCrafters.

**Subject of audit (competitor):** OpticLeague
**Phase:** 0 — Competitor Website Audit
**Audit date:** 2026-07-08
**Audited properties:** `www.opticleague.com` (desktop), `m.opticleague.com` (mobile), `api.opticleague.com` (API), `image.opticleague.com` (asset CDN)
**Method:** HTTP inspection, raw HTML analysis, JavaScript bundle route/endpoint extraction, public read-only API content retrieval, CSS palette extraction. No authenticated areas were accessed; no write operations were performed. A rendered-browser visual pass was not possible in this environment (no connected browser), so visual findings are derived from shipped CSS, markup, and component libraries — noted where applicable.

---

## 1. Executive Summary

The current OpticLeague website is **not a marketing website with weak visuals — it is a live, transactional, single-vendor B2B wholesale eyewear ordering portal** built as two separate client-rendered Vue.js single-page applications (desktop + mobile), backed by a private JSON API that appears to be shared with an existing ERP (`img.erp.lensmartonline.com` appears in live product data, indicating shared infrastructure with the Lensmart consumer eyewear operation).

**What works today (and must not be broken):** a functioning wholesale commerce flow — catalog with 8 optical categories, tiered wholesale pricing, MOQs, prescription-lens SKUs (SPH/CYL), custom logo engraving, cart, order creation, bank-transfer and online payment, refunds, logistics tracking, buyer company verification, loyalty/member levels, favorites, and re-purchase.

**What fails today:** essentially everything the brief targets. The site is invisible to search engines (client-rendered, one-URL sitemap, every page titled "Welcome to OpticLeague"), has no supplier/marketplace layer, no RFQ, no trust infrastructure, no brand identity beyond a default component library, an accessibility posture with hard WCAG failures, and a duplicated two-codebase frontend architecture held together by user-agent sniffing redirects.

**Strategic finding:** the gap between the current site (single-vendor wholesale shop) and the product vision (multi-supplier global optical network with RFQ, verification, and procurement) is a **product gap, not just a design gap**. The redesign must (a) preserve the working order flow for existing customers, (b) introduce the marketplace/RFQ/supplier layer as genuinely new construction, and (c) replace the frontend and public-web architecture entirely.

---

## 2. Detected Technical Architecture

### 2.1 Frontend — desktop (`www.opticleague.com`)

| Aspect | Finding |
|---|---|
| Framework | Vue 3 SPA, Vite build (`/assets/index-*.js`, ~291 KB main chunk + ~95 lazy chunks) |
| UI library | Element Plus (default theme, `el-*` chunks: button, dialog, form, select, pagination, popover, etc.) |
| Other libs | lodash, Swiper, axios; custom `translator-worker` (client-side machine translation in a Web Worker) |
| Rendering | 100% client-side. Initial HTML is an empty `#app` shell with a CSS text-loader animation ("OpticLeague" underline wipe) |
| Server | nginx; HTTP/3 advertised (`Alt-Svc`); HSTS enabled |
| Mobile handling | UA-sniffing **302 redirect** to `m.opticleague.com` for non-desktop user agents (including default `curl`) |
| Error recovery | Inline script re-fetches `/version.json` and re-injects assets on chunk-load failure (deploy-cache mitigation — indicates real deployment pain) |
| Fonts | Inter (regular weight only), self-hosted woff2; contradictory `font-display: swap` + `font-display: optional` declarations in the same `@font-face` |
| Body markup | `<body style="overflow: hidden; width: calc(100% - 15px);">` — a hardcoded scrollbar-width hack |

### 2.2 Frontend — mobile (`m.opticleague.com`)

A **completely separate second Vue 3 SPA**: Vant UI, vue-i18n (locale effectively empty/EN-only), Pinia, VueUse, jsPDF, axios. Same analytics stack, separate Clarity project. Every feature is implemented twice across the two apps.

### 2.3 API (`api.opticleague.com`)

- JSON envelope convention `{code: 1|0, msg, data}` (typical of PHP/ThinkPHP-family admin frameworks).
- Public, unauthenticated read endpoints observed in use by the site: `index/index`, `index/getBannerList`, `index/getAsideList`, `index/getFooterList`, `index/getWebsiteConfig`, `goods/getGoodsList`, `goods/getGoodsDetail`, `goods/getFilter`, `goods/getLatestGoods`, `goods/getRandomGoods`, `news/getNewsLists`, `news/getNewsDetail`, `news/getNewsType`, `common/getCurrencyList`, `common/getSimpleCountryList`, `common/getSimpleStateList`.
- Authenticated/transactional endpoints referenced by the code: cart, order lifecycle (create/cancel/confirm/refund/repurchase/logistics), pay (`payOrder`, `getTradeStatus`), user profile/certification/wish, `common/uploadImg`.
- **No API versioning** (`/v1/`) and no visible rate limiting on public endpoints.
- Product payloads expose internal identifiers (`factory_goods_id`, `factory_sku_id`, per-factory image paths `B2B/factory/{id}/…`) and a second company's ERP image domain (`img.erp.lensmartonline.com`).

### 2.4 Analytics & marketing

- Google Analytics 4 (`G-Z2RXN8PXLW`) on both properties.
- Microsoft Clarity — two projects (`rflbb5pqjr` desktop, `rh9h6wettp` mobile).
- Meta Pixel (`1533581021081743`).
- No cookie-consent mechanism was detected in the shipped markup — all trackers fire unconditionally on load (GDPR/ePrivacy exposure for EU buyers).

### 2.5 SEO infrastructure

- `robots.txt`: allows all; points to sitemap.
- `sitemap.xml`: **contains exactly one URL** (`/`), `changefreq: daily`, `priority: 1.0`.
- `<title>` and `<meta name="description">`: `"Welcome to OpticLeague"` — identical on every route (SPA never updates them per page in the shipped HTML).
- No Open Graph, no Twitter cards, no structured data, no canonical tags, no `hreflang`, no `rel=alternate` pairing between `www.` and `m.` hosts.

---

## 3. Current Sitemap (reconstructed from router)

```
/                                   Home (banners, quick links, recommended goods)
/products                           Catalog hub
/products/category/:categoryId      Category listing (filterable, paginated)
/products/details/:detailId         Product detail (SKUs, tiered prices, measurements)
/products/classify                  Category index ("all categories")
/products/search                    Search results
/products/Latest                    New arrivals
/cart                               Shopping cart
/create_order                       Checkout / order creation
/tobepaid                           Awaiting-payment orders
/payment_success/:order_no          Payment confirmation
/payment_transfer/:order_no         Bank-transfer instructions
/export_pdf                         Order PDF export
/login  /register  /forget          Auth (login, registration, password reset)
/user                               Account dashboard
/user/info                          Profile
/user/address                       Address book
/user/collection                    Favorites / wishlist
/user/company-auth                  Buyer company verification (business docs)
/user/logo                          Custom logo management (engraving service)
/user/order/:type                   Order list by status
/user/order/:id                     Order detail
/about-us                           About page
/news                               News hub (types: Brand / Industry / Exhibition)
/news/:newsId                       Article detail
/faq                                FAQ
/privacy-policy                     Privacy policy
/:path(.*)*                         404 catch-all
```

**Notably absent:** terms of service, cookie policy, accessibility statement, contact page, pricing/how-it-works, supplier-facing anything, RFQ anything, company legal identity/imprint.

---

## 4. Existing Features (functional inventory)

### 4.1 Catalog & product model (the strongest existing asset)

Top-level taxonomy (live from API):

1. **Eyeglasses** (All / Women / Men / Kids)
2. **Sunglasses** (All / Women / Men / Kids)
3. **Reading Glasses** (All / Women / Men)
4. **Sports Glasses** (Cycling / Skiing & Snowboarding / Swimming / Running / Climbing)
5. **Functional Glasses** (Night Vision / Safety / Clip-on)
6. **Lenses** (Single Vision / Blue Light Blocking / Prescription Colored)
7. **Contact Lenses** (Colored)
8. **Tools & Accessories** (Accessories / Eyecare Products / Repairing Tools / Assembling Tools)

Product data model (live from `goods/getGoodsDetail`): SKU code naming (`OLE…` eyeglasses, `OLS…` sunglasses, `OLC…` contacts), color SKUs with images, **tiered wholesale pricing (`price_layer`)**, **MOQ (`start_num`, e.g. 6 units)**, stock/frozen-stock, **frame measurements** (lens width/height, bridge width, frame width, temple length), weight, **prescription parameters (SPH/CYL) on lens SKUs**, **logo-customization flag** (`is_support_logo`, priced ≈ $0.26/unit via site config), invoice tax rate (13% — Chinese VAT), breadcrumbs, wishlist state.

Unit prices observed: ≈ $1.47–$9.27 — genuine factory-wholesale positioning.

### 4.2 Commerce flow

Cart → order creation → payment (online + **bank transfer** with dedicated instruction page) → order statuses → logistics tracking (`getAddressLogistic`) → refund flow (`RefundedDialog`, `getOrderRefund`) → re-purchase → order PDF export. Currency list + country/state lists; freight multiplier (`freight_multiple: 1.5`) in site config.

### 4.3 Account & trust primitives

Buyer registration, email verification codes, password reset, profile management, address book, favorites, **company verification** (`company-auth`, `userAuthentication`, `getUserCertification` — an embryo of the "verified buyer/supplier" concept), **member levels / loyalty rewards** (member-level artwork in site config; "Loyalty Rewards" footer link; "Members Only: More Purchases, Bigger Discounts!" merchandising on every product card).

### 4.4 Content

- **News** hub with three editorial types (Brand / Industry / Exhibition); mix of first-party guides ("How to Buy Eyewear on Our Website?") and syndicated industry items (Zeiss, EssilorLuxottica — credited "VM", i.e. Vision Monday).
- **Buyer's Guide PDF** (hosted on image CDN, filename indicates an internal draft: `Buyer's-Guide-updated-Draft-4.14.pdf`).
- **FAQ** page.
- Homepage banners cross-promote **World Optics Fair (`opticsfair.com`)** — an apparently affiliated property.

### 4.5 Internationalization (current state)

- Desktop: a client-side **machine-translation Web Worker** (`translator-worker`) that rewrites the DOM — not real i18n. Invisible to search engines, quality-uncontrolled.
- Mobile: vue-i18n present but effectively EN-only.
- Admin data contains Chinese-language internal names ("光学", "墨镜", "新品") leaking into the public banner API — the back office operates in Chinese.

---

## 5. Content Inventory

| Content | Location | Quality | Disposition |
|---|---|---|---|
| Category taxonomy (8 groups, 30+ subcategories) | API | Good, optical-specific | **Preserve** (re-map into new IA) |
| Product data: measurements, tiered prices, MOQ, Rx SKUs, logo service | API/ERP | Strong structurally; names are opaque SKU codes; descriptions ~empty | **Preserve data, rewrite presentation** |
| ~9,000+ product records (IDs observed up to ≥ 9001) | ERP | Photography inconsistent (mixed factory uploads) | **Preserve; standardize imagery over time** |
| News articles (100+, three types) | API | Mixed: useful first-party guides; syndicated items have thin/empty descriptions and third-party sourcing | **Preserve first-party; review/rewrite syndicated items (rights + quality)** |
| Buyer's Guide PDF | CDN | Useful concept; "Draft" filename in production | **Rewrite as on-site content (HTML), keep PDF as secondary** |
| FAQ | SPA page | Not fully auditable without render | **Preserve substance, rewrite copy** |
| About Us | SPA page | Not fully auditable without render; homepage quick-link labels it "Profile" | **Rewrite entirely** |
| Privacy Policy | SPA page | Sole legal document on site | **Rewrite; add Terms, Cookies, Accessibility, Imprint** |
| Homepage banners | API | Ad-hoc JPGs incl. third-party fair promo | **Remove format; rebuild as designed sections** |
| "Members Only: More Purchases, Bigger Discounts!" card blurb | API (every product) | Repetitive, weakens credibility | **Remove; replace with per-product substance (MOQ, lead time, tiers)** |
| Loyalty/member-level artwork | Site config | Concept worth keeping | **Preserve concept, redesign** |
| Meta titles/descriptions | HTML | "Welcome to OpticLeague" everywhere | **Remove/replace globally** |

---

## 6. Problems

### 6.1 UX problems

1. **No orientation or value proposition.** The home experience is banners + SKU grids; nothing explains what OpticLeague is, who it serves, MOQs, shipping, or how to start.
2. **Opaque product identity.** Products are named by SKU code (`OLE77767041`) with empty `sub_name`/`desc` — buyers must decode images alone.
3. **Wholesale mechanics hidden until deep in the flow.** MOQ (6 pcs), tiered pricing, freight multiplier, and 13% invoice tax surface only at detail/checkout.
4. **No buyer journey for non-transactional intent.** No contact page, no sales inquiry path, no RFQ — a buyer who wants 5,000 custom units has the same UI as one buying 6.
5. **Account-gated basics.** "Loyalty Rewards" footer link points to `/login`; value is asserted but never explained publicly.
6. **Two divergent experiences** (desktop vs mobile app) with different UI kits and capabilities.
7. **Search/filtering thin:** `goods/getFilter` returns empty attribute/color facets in practice; the rich lens parameters (SPH/CYL, materials, coatings) are not searchable.

### 6.2 UI / visual identity problems

1. **No design system.** Element Plus (desktop) and Vant (mobile) defaults; palette extracted from shipped CSS is an unharmonized mix: dark navy `#222f3e`, four near-identical oranges (`#ff6b26/#ff6e27/#ff7f28/#ff8a3c/#ffa32b`), gold `#febd68`, two blues (`#306bbb/#6b8eb7`), plus component-library defaults (`#f56c6c` error red) and ad-hoc grays.
2. **Single font weight.** Inter Regular only — no typographic hierarchy is even possible; headings fall back to system faux-bold.
3. **Viewport-relative typography** (`font-size: 1.77083vw` on the loader and elsewhere) — magic numbers from a fixed-canvas design; breaks at large/small screens and when zooming.
4. **No brand assets beyond a favicon and a loading GIF** (`logo_loading4.gif`). No logo system, no iconographic language, no photography direction (mixed factory photos on white).
5. First impression is a **text-loader animation on a blank screen** — the brand is literally "loading" on arrival.

### 6.3 Accessibility problems (hard failures)

1. `user-scalable=no, maximum-scale=1.0` on mobile — **blocks pinch zoom (WCAG 1.4.4 failure)**.
2. Client-rendered empty shell: **no content without JavaScript**; `<noscript>` contains only a Meta Pixel image.
3. Loading state has **no `aria-live`/role** — screen readers get silence, then a full DOM swap.
4. vw-based font sizing undermines browser text-size settings (WCAG 1.4.4).
5. `overflow: hidden` on `<body>` plus custom scroll math is hostile to keyboard/AT scrolling.
6. No skip links, no visible focus strategy, no reduced-motion handling detected in shipped CSS.
7. Machine-translation DOM rewriting (desktop translator worker) breaks `lang` attribution — screen readers announce translated text with wrong pronunciation rules.

### 6.4 Performance problems

1. **CSR-only critical path:** empty HTML → main JS (291 KB) → route chunk → API round-trips (4+ calls for home) → render. LCP is unachievable in the target range on slow networks; FCP is a loader animation.
2. ~95 JS chunks with hash-failure self-repair script — evidence of chunk-churn deploy issues.
3. Full Element Plus + lodash + Swiper payload for a catalog page.
4. Images: original-size JPG/PNG from `image.opticleague.com` with **no responsive `srcset`, no AVIF/WebP pipeline** (mixed formats present but not systematic), no width/height reservation (CLS risk).
5. Client-side translation worker adds CPU cost to every page for non-EN users.
6. Three trackers load on first paint on both properties.

### 6.5 SEO problems (existential)

1. Every URL renders server-side as the same empty document titled **"Welcome to OpticLeague"** — the entire catalog, news hub, and all landing surfaces are effectively invisible or duplicate to crawlers.
2. Sitemap lists a single URL; no per-page canonicals.
3. UA-sniffing 302 redirect to `m.opticleague.com` with **no `rel=alternate`/`canonical` pairing** — classic duplicate-host penalty setup, and 302 (not 301) semantics.
4. Zero structured data (no Organization, Product, Breadcrumb, Article schema).
5. Zero Open Graph/Twitter metadata — links shared in WhatsApp/WeChat/LinkedIn (the actual B2B channels) render blank.
6. No `hreflang`, and machine-translated DOM text is invisible to crawlers anyway.
7. News content partially syndicated with thin descriptions — no canonical attribution strategy.

### 6.6 Trust & credibility problems

1. **No company identity:** no legal entity name, address, registration, or imprint anywhere in shipped markup; the only "About" entry point is labeled "Profile".
2. **No people, no story, no factory evidence** — for a factory-direct value proposition, the strongest possible trust content (manufacturing capability) is absent.
3. **Internal leakage:** public API exposes `factory_goods_id`, per-factory image paths, and a different company's ERP domain (`img.erp.lensmartonline.com`) — discoverable by any technical buyer and undermines the independent-platform positioning.
4. Production assets include a file literally named "…Draft…".pdf.
5. Repetitive merchandising copy ("Members Only…") on every card reads as template filler.
6. No certifications (CE/FDA/ISO), no compliance documentation, no quality-assurance narrative — table stakes in optical B2B.
7. Unconditional third-party tracking without consent management (EU buyer trust + legal).

### 6.7 Conversion problems

1. Single conversion path (register → order). No low-commitment paths: no inquiry form, no RFQ, no sample-request framing, no WhatsApp/WeChat contact, no email capture beyond `recordEmail` (newsletter endpoint exists but has no visible strategy).
2. Anonymous visitors see prices but no explanation of membership tiers — the primary incentive ("bigger discounts") is a dead end without context.
3. No urgency/logistics clarity pre-checkout (lead times absent from the product model presentation).
4. Registration wall friction is uncalibrated: company verification exists but its benefits are never sold.
5. No analytics-driven funnels evident (events beyond pageview not detected in shipped code).

---

## 7. What to Preserve / Rewrite / Remove

**Preserve (data & concepts):**
- Category taxonomy and the full product data model (measurements, Rx parameters, tiered pricing, MOQ, stock, logo service) — this is genuine optical-industry specialization and maps directly onto the new lens/frame discovery vision.
- The working order/payment/refund/logistics flow (business continuity — existing customers transact today).
- Company-verification concept → seed of the platform "verification" module.
- Member/loyalty concept → future buyer-tier programs.
- First-party editorial content (buying guides) and the News hub structure (Brand/Industry/Exhibition → maps to the "Industry Intelligence" vision).
- Analytics continuity (GA4/Clarity/Pixel IDs) — reinstall behind consent management.

**Rewrite:**
- Every line of interface copy and all page-level content (About, FAQ, guides).
- Product presentation layer: human product names/descriptors alongside SKU codes; surfaced MOQ/tiers/lead-time.
- Privacy policy; produce Terms, Cookie policy, Accessibility statement, company imprint.
- News/syndication approach (rights-cleared, canonical-attributed, substantial summaries).

**Remove:**
- The `m.` mobile-site architecture and UA-sniffing redirects (one responsive codebase).
- The client-side machine-translation worker (replace with real i18n).
- "Welcome to OpticLeague" placeholder metadata everywhere.
- Banner-driven homepage merchandising and per-card filler copy.
- Public exposure of internal ERP identifiers/domains (API response shaping).
- `user-scalable=no`, body scroll hacks, vw-typography.

---

## 8. Recommended New Information Architecture

```
PUBLIC WEB (SSR, indexable, i18n-ready: en / fr / zh-Hans / de)
├── /                        Positioning + network narrative + marketplace preview
├── /marketplace             Discovery hub (categories, featured suppliers, search)
├── /products                Product index
│   ├── /products/lenses     Signature lens discovery (index, material, design,
│   │                        coating, photochromic, Rx range, MOQ, lead time)
│   ├── /products/frames     Frames (incl. current Eyeglasses/Sunglasses/Reading/
│   │                        Sports/Functional taxonomy as facets)
│   ├── /products/equipment  Equipment & lab supplies (incl. Tools & Accessories)
│   └── /products/[slug]     Product detail (spec tables, tiers, MOQ, documents)
├── /suppliers               Supplier directory (verification-first)
│   └── /suppliers/[slug]    Supplier profile (capabilities, certs, export markets)
├── /rfq                     RFQ explainer + submission (public lead-gen v1)
├── /solutions               (+ /retailers /distributors /manufacturers …)
├── /industries              Industry landscape pathways
├── /resources               Industry intelligence (news, guides, reports)
│   └── /resources/[slug]    Article detail (Article schema, canonical sourcing)
├── /about  /contact  /pricing
├── /login  /register
└── /privacy /terms /cookies /accessibility  + 404/500/empty/loading states

AUTHENTICATED APP (existing commerce, redesigned within the same design language)
└── /account → dashboard, orders (list/detail/tracking/refunds/PDF), cart,
    checkout, payment & transfer instructions, addresses, favorites,
    company verification, logo assets, membership
```

Mapping principle: **every current URL 301-redirects into the new IA** (e.g. `/products/category/34` → `/products/frames/eyeglasses`), preserving the live business and any existing link equity.

---

## 9. Recommended Technical Architecture (summary — full ADRs in Phase 1/2)

| Layer | Recommendation | Rationale vs. current state |
|---|---|---|
| Public web | **Next.js (App Router, RSC, TypeScript strict), one responsive codebase, Docker-deployable** | Fixes the existential SEO/performance failure; replaces the duplicated desktop+mobile SPAs |
| Design system | OpticLeague-owned tokens/components (CSS variables; Radix/shadcn primitives wrapped) | Replaces raw Element Plus/Vant defaults; enables the award-caliber visual language |
| Backend | **Java 25 LTS + Spring Boot modular monolith** (identity, organizations, catalog, optical-lenses, rfq, quotations, orders, verification, content, …) | Clean system of record to replace/encapsulate the current PHP-style ERP API; boring, 10–20-year stack |
| Data | PostgreSQL + Flyway; UUIDv7 keys; relational lens/frame spec model | The current rich product model deserves relational integrity + faceted search (PG FTS/trigram first) |
| API | REST + OpenAPI 3.1, versioned (`/v1`), typed frontend client generated from contract | Replaces unversioned `{code,msg,data}` endpoints; stops internal-ID leakage by design |
| Migration posture | New platform runs alongside the existing ERP; catalog/content data imported via ETL; order flow cut over last | Protects the live transactional business |
| i18n | Server-rendered locale routing + translation files (en/fr/zh-Hans/de), `hreflang` | Replaces the client-side machine-translation worker |
| Compliance | Consent management before trackers; cookie/terms/accessibility pages | Closes the GDPR gap |

---

## 10. Open Questions for Stakeholders (blocking items flagged before Phase 1)

1. **Business continuity:** Are there active customers ordering through the current site? (Evidence says yes.) What is the acceptable cutover strategy and freeze window?
2. **ERP relationship:** The catalog lives in an ERP shared with `lensmartonline.com`. Is the new platform expected to keep consuming that ERP (integration layer), or become the system of record (migration)?
3. **Marketplace reality:** The vision is multi-supplier with RFQ. Today the data shows multiple internal "factories" behind one storefront. Should Phase 1 present suppliers as first-class public entities, and are those factories willing to be named/verified?
4. **Verified metrics:** No production metrics may be fabricated. Which real numbers (products listed, factories, countries served, years operating) are approved for public use? Until supplied, the design will ship with clearly-labeled placeholder tokens.
5. **Brand assets:** Does a logo/brand kit exist beyond the favicon? Is the OpticLeague ↔ World Optics Fair relationship official (co-branding opportunity)?
6. **News syndication rights:** Confirm licensing for Vision Monday-sourced items before migrating them.
7. **Pricing visibility policy:** Wholesale prices are currently public. Keep public, gate behind login, or gate behind verified-buyer status?

---

*End of Phase 0 audit. No production code has been written. Implementation is blocked pending approval of this document, the proposed IA (§8), and the technical architecture direction (§9).*
