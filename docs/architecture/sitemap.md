# D — Proposed Sitemap & Information Architecture

**Project:** SpexCrafters · **Date:** 2026-07-08
**Frontend convention:** Next.js App Router. Locale-prefixed public routes (`/{locale}/…`, default `en` unprefixed or via negotiation — final call in ADR-010). Route groups: `(public)`, `(auth)`, `(buyer)`, `(supplier)`, `(admin)`.

## D.1 Public website (SSR/SSG, indexable, localized)

```
/                                   Homepage (positioning, search, discovery rails)
/marketplace                        Discovery hub (categories × suppliers × RFQ entry)
/products                           Product index (all verticals)
/products/[slug]                    Product detail page (PDP)
/categories                         Category index (full taxonomy)
/categories/[slug]                  Category landing (facets, SEO copy, subcats)
  e.g. /categories/ophthalmic-lenses, /categories/frames, /categories/machinery
/suppliers                          Supplier directory (facets: type, country, verified, certs)
/suppliers/[slug]                   Supplier profile
/brands                             Brand index (house/OEM brands)
/brands/[slug]                      Brand landing
/rfq                                RFQ marketplace explainer + public RFQ board (gated detail)
/rfq/new                            RFQ creation (auth-gated at submit, form public)
/insights                           Editorial hub (industry, guides, brand news)
/insights/[slug]                    Article
/events                             Trade fairs & exhibitions
/events/[slug]                      Event detail
/pricing                            Supplier plans & buyer FAQ
/verification                       Verified Supplier Program explainer
/how-it-works                       Buyer + supplier flows explained
/about                              Company, story, legal identity
/contact                            Contact / sales inquiry
/legal/terms  /legal/privacy  /legal/cookies  /accessibility  /imprint
/search                             Global search results (products/suppliers/RFQs/content)
/sitemap.xml (segmented: products, suppliers, categories, insights, events)
/robots.txt
404 / 410 / 500 designed states
```

**Indexing policy:** PDPs, supplier profiles, category landings, insights, events = indexable. `/search` and arbitrary facet combinations = `noindex,follow`; a curated allowlist of category × high-value-attribute pages (e.g. `/categories/ophthalmic-lenses/1-67-index`) are promoted to static, indexable landing pages. RFQ details = noindex (business-sensitive).

## D.2 Auth

```
/auth/login          /auth/register       /auth/verify-email
/auth/forgot-password  /auth/reset-password
/auth/mfa            (TOTP challenge + enrolment)
/auth/select-org     (multi-organization chooser)
/auth/accept-invite  (employee invitation landing)
```

## D.3 Buyer portal `/buyer/*` (client-auth’d, non-indexable)

```
/buyer                          Dashboard (open RFQs, new quotations, unread messages)
/buyer/rfqs                     RFQ list (draft/open/awarded/closed)
/buyer/rfqs/new                 RFQ wizard
/buyer/rfqs/[id]                RFQ detail + quotation inbox
/buyer/rfqs/[id]/compare        Quotation comparison
/buyer/quotations               All received quotations
/buyer/messages                 Conversation list
/buyer/messages/[id]            Thread
/buyer/favorites                Saved products & suppliers
/buyer/saved-searches           Saved searches + alert settings
/buyer/organization             Org profile, verification status
/buyer/organization/team        Members & roles
/buyer/notifications            Notification center
/buyer/settings                 Profile, security (MFA), locale/currency prefs
```

## D.4 Supplier portal `/supplier/*`

```
/supplier                       Dashboard (RFQ matches, quote statuses, profile views)
/supplier/onboarding            Multi-step wizard (company → capabilities → certs → docs)
/supplier/company               Public-profile editor (preview mode)
/supplier/verification          Verification status & document management
/supplier/products              Catalog management (list, status, bulk actions)
/supplier/products/new          Product editor (category → attribute template → media)
/supplier/products/[id]         Edit product
/supplier/rfqs                  RFQ discovery (browse / matched / invited)
/supplier/rfqs/[id]             RFQ detail + quote composer
/supplier/quotations            Submitted quotations (statuses, revisions)
/supplier/quotations/[id]       Quotation detail / revise / withdraw
/supplier/messages  /supplier/messages/[id]
/supplier/analytics             Profile views, search appearances, RFQ funnel
/supplier/team                  Members & roles
/supplier/notifications  /supplier/settings
```

## D.5 Admin portal `/admin/*` (separate layout, hardened access)

```
/admin                          Ops dashboard
/admin/users                    User management
/admin/organizations            Org management (buyers + suppliers)
/admin/verification             Verification queue (review, approve/reject, history)
/admin/products                 Product moderation queue + catalog oversight
/admin/taxonomy/categories      Category management
/admin/taxonomy/attributes      Attribute-definition registry
/admin/rfqs                     RFQ moderation
/admin/quotations               Quotation oversight
/admin/reports                  Abuse reports
/admin/messaging                Conversation moderation
/admin/content/pages            CMS pages
/admin/content/insights         Articles
/admin/content/events           Events
/admin/content/homepage         Homepage sections, featured suppliers/products
/admin/seo/metadata             Metadata overrides
/admin/seo/redirects            Redirect manager (301/410)
/admin/localization             Translation management
/admin/currencies               Currencies & exchange rates
/admin/notifications/templates  Email & notification templates
/admin/audit-logs               Audit log explorer
/admin/security                 Security events, suspicious logins
/admin/configuration            Platform config
/admin/feature-flags            Feature flags
/admin/analytics                Platform analytics
```

## D.6 Next.js route-group layout plan

```
apps/web/src/app/
  [locale]/
    (public)/    → marketing + marketplace layout (header/mega-menu/footer)
    (auth)/      → minimal centered layout
    (buyer)/buyer/     → app shell (sidebar, org switcher)  [server-enforced session + role]
    (supplier)/supplier/ → app shell                          [idem]
    (admin)/admin/     → admin shell                          [idem + platform role]
  api/ (BFF route handlers: auth callbacks, session, uploads proxy)
```

Every layout enforces authorization server-side (session check + role check via backend); frontend guards are UX only, never security (per §8 of the master brief).

## D.7 URL & slug rules

- Slugs: lowercase, hyphenated, unique per entity type, immutable after publication (changes create a 301 via the redirect manager).
- Products: `/products/{supplier-slug}-{product-slug}` collision-safe form if needed; canonical always self-referencing.
- Localized slugs permitted for categories/insights (per-locale slug column); products/suppliers share slugs across locales with `hreflang` alternates.
- All legacy/renamed URLs handled by the admin redirect manager (301 permanent, 410 for removals).
