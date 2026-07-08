# SpexCrafters — User Journeys

This document specifies the end-to-end user journeys for the SpexCrafters B2B optical-industry marketplace (spexcrafters.com). It is the shared reference for product, design, frontend (Next.js App Router, SSR public web), backend (Java 25 / Spring Boot modular monolith, PostgreSQL, REST/OpenAPI), and QA when building, reviewing, or instrumenting flows. Each journey states the persona and goal, entry points, preconditions, a numbered step table (surface/route, user action, system behavior), success criteria, failure and edge states, the notifications/emails triggered, and the KPIs to instrument. Journeys are written against the agreed portal structure (public site, `/buyer/*`, `/supplier/*`, `/admin/*`) and role model (Visitor; Authenticated User; org roles `ORG_OWNER`/`ORG_ADMIN`/`ORG_MEMBER`; capability roles `BUYER_PROCUREMENT`, `SUPPLIER_SALES`, `SUPPLIER_CATALOG`; platform roles `SUPER_ADMIN`, `PLATFORM_ADMIN`, `PLATFORM_MODERATOR`, `PLATFORM_SUPPORT`, `PLATFORM_AUDITOR`). Where the incumbent audit (`CURRENT_SITE_AUDIT.md`) identified failure patterns to avoid (SEO-invisible SPA, single conversion path, hidden wholesale mechanics), the relevant journey notes the corrective behavior.

## Journey index

| ID | Journey | Primary persona | Primary surfaces |
|----|---------|-----------------|------------------|
| [V1](#v1-seo-landing-on-productcategory-page--soft-conversion) | SEO landing → explore → soft conversion | Visitor (search-originated buyer) | `/products/[slug]`, `/categories/[slug]`, `/rfq/new`, `/auth/register` |
| [V2](#v2-homepage-first-visit--value-prop--supplier-discovery) | Homepage first visit → supplier discovery | Visitor (referred/direct) | `/`, `/suppliers`, `/suppliers/[slug]` |
| [V3](#v3-search-driven-product-discovery-with-faceted-filtering) | Faceted product search with shareable URL state | Visitor or authenticated buyer | `/marketplace`, `/products`, `/categories/[slug]` |
| [B1](#b1-buyer-registration-email-verification-organization-creation) | Registration + email verification + org creation | New buyer user | `/auth/register`, `/buyer/dashboard` |
| [B2](#b2-buyer-onboarding-and-company-verification) | Buyer onboarding + company verification | ORG_OWNER (buyer org) | `/buyer/organization`, `/verification` |
| [B3](#b3-product-discovery--detail--favorite--contact-supplier) | Discover → detail → favorite → contact supplier | BUYER_PROCUREMENT | `/products/[slug]`, `/buyer/favorites`, `/buyer/messages` |
| [B4](#b4-create-and-publish-a-public-rfq) | Create & publish public RFQ | BUYER_PROCUREMENT | `/rfq/new`, `/buyer/rfqs` |
| [B5](#b5-private-rfq-to-selected-suppliers) | Private RFQ to invited suppliers | BUYER_PROCUREMENT | `/rfq/new`, `/buyer/rfqs`, supplier portal |
| [B6](#b6-receive-quotations--compare--negotiate--award--close) | Quotations → compare → negotiate → award → close | BUYER_PROCUREMENT | `/buyer/rfqs/[id]`, `/buyer/quotations`, `/buyer/messages` |
| [B7](#b7-saved-search-alert--notification--return-visit) | Saved-search alert → return visit | BUYER_PROCUREMENT | `/buyer/saved-searches`, email, `/products` |
| [S1](#s1-supplier-registration-organization-creation-onboarding-wizard) | Supplier registration + onboarding wizard | New supplier user | `/auth/register`, `/supplier/onboarding` |
| [S2](#s2-verification-submission--admin-review--verified-badge) | Verification submission → verified badge | ORG_OWNER/ORG_ADMIN (supplier) | `/supplier/company`, `/admin/verification` |
| [S3](#s3-product-creation-with-category-specific-technical-attributes) | Product creation with technical attributes & media | SUPPLIER_CATALOG | `/supplier/products` |
| [S4](#s4-rfq-discovery--submit-quotation) | RFQ discovery → submit quotation | SUPPLIER_SALES | `/supplier/rfqs`, `/supplier/quotations` |
| [S5](#s5-quotation-revision-and-withdrawal) | Quotation revision after negotiation; withdrawal | SUPPLIER_SALES | `/supplier/quotations/[id]`, `/supplier/messages` |
| [S6](#s6-team-management-invite-employee-assign-roles) | Team management: invite + roles | ORG_OWNER/ORG_ADMIN | `/supplier/team` (mirrored in `/buyer/organization`) |
| [A1](#a1-supplier-verification-review) | Supplier verification review | PLATFORM_ADMIN | `/admin/verification` |
| [A2](#a2-product-moderation-queue) | Product moderation queue | PLATFORM_MODERATOR | `/admin/moderation/products` |
| [A3](#a3-rfqspam-moderation-abuse-reports-account-suspension) | RFQ/spam moderation, abuse reports, suspension | PLATFORM_MODERATOR / PLATFORM_ADMIN | `/admin/moderation`, `/admin/reports`, `/admin/organizations` |
| [X1](#x1-password-reset) | Password reset | Any registered user | `/auth/login`, email |
| [X2](#x2-mfatotp-enrolment) | MFA/TOTP enrolment | Any registered user | `/buyer/settings` or `/supplier/settings` |
| [X3](#x3-multi-organization-switching) | Multi-organization switching | User in ≥2 orgs | Global org switcher, portal shells |
| [X4](#x4-messaging-thread-lifecycle) | Messaging lifecycle: attachments, read state, block/report | Buyer + supplier participants | `/buyer/messages`, `/supplier/messages` |

Conventions used below:

- **Surface/route** refers to the Next.js route the user is on; API interactions are described as system behavior, not enumerated per endpoint.
- All authenticated portal routes enforce organization scoping server-side; role names in preconditions are the minimum capability required.
- "Notification" means an in-app notification record; "email" means a transactional email. Both respect the user's notification preferences and locale (en, zh-Hans, fr, de).
- Monetary values are stored in the quotation's currency and displayed with multi-currency conversion for display only (rate + timestamp disclosed).

---

## Visitor journeys

### V1. SEO landing on product/category page → soft conversion

**Persona & goal.** A procurement manager at an optical retail chain lands from a search engine on a product detail or category page (e.g. "1.67 high-index blue-cut lenses wholesale"). Goal: judge relevance quickly, explore, and take a low-commitment next step (RFQ or registration) without being forced into an account. This journey directly corrects the incumbent's existential SEO failure (client-rendered shell, identical titles) and its single register-to-order conversion path.

**Entry points.**
- Organic search result → `/products/[slug]` or `/categories/[slug]` (SSR, unique title/meta, Product/BreadcrumbList structured data, `hreflang` alternates).
- Shared link (WhatsApp/WeChat/LinkedIn) rendering Open Graph preview.

**Preconditions.** None. Anonymous session. Product/category is published and indexable.

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/products/[slug]` | Arrives from SERP | SSR page renders full content: product name, spec table (e.g. lens index, coating stack, Rx range), MOQ, lead time, price-tier display per pricing-visibility policy, supplier card with verification badge, breadcrumbs. Locale resolved from URL prefix/`Accept-Language`. |
| 2 | `/products/[slug]` | Scans spec table and supplier card | Related products and "more from this supplier" sections render server-side; currency selector applies display conversion client-side. |
| 3 | `/products/[slug]` | Clicks breadcrumb or category chip | Navigates to `/categories/[slug]` with the product's category context; faceted listing SSR-rendered. |
| 4 | `/categories/[slug]` | Applies 1–2 facets, opens 1–2 more products | Filter state serialized to URL query (see V3); listing updates; each product URL is canonical and indexable. |
| 5 | `/products/[slug]` | Clicks a soft-conversion CTA: "Request a Quote", "Contact supplier", or "Save" | RFQ CTA → `/rfq/new` prefilled with product/category context, usable anonymously up to the review step. Contact/Save → auth gate with context-preserving `returnTo`. |
| 6a | `/rfq/new` | Fills RFQ essentials (specs prefilled), reaches submit | System requires account at submission: inline register/login step; draft preserved through auth (server-side draft keyed to session). Continues in B4. |
| 6b | `/auth/register` | Registers to save/contact | Registration flow (B1); on completion, user is returned to the originating product with the intended action (favorite/contact) resumed. |

**Success criteria.**
- Visitor reaches a second content page (explore) and initiates RFQ, registration, or supplier contact in the same session.
- No dead ends: every gate preserves context and returns the user to their task after auth.
- Page is fully readable without JavaScript execution (SSR content); Core Web Vitals within targets on the landing template.

**Failure & edge states.**
- Product unpublished/removed → SSR 410/404 page with category-level alternatives and search box (no blank shell).
- Product exists but supplier suspended → product page hidden from index; direct hits show "no longer available" with related listings.
- Slug changed → 301 redirect from historical slug (slug history table).
- Pricing gated by policy → tiers replaced by "Sign in to view wholesale tiers" with explicit explanation of what registration unlocks (avoid the incumbent's unexplained login walls).
- RFQ draft abandoned mid-auth → draft retained (TTL) and offered on next visit.

**Notifications/emails.** None until conversion (registration email in B1; RFQ confirmations in B4). Supplier receives a "product page inquiry" notification only when contact/RFQ completes.

**KPIs to instrument.**
- Organic landing sessions per template (product/category); SERP CTR via Search Console pairing.
- Landing → second-page rate; landing → CTA-click rate by CTA type (RFQ / contact / register / save).
- RFQ-draft start rate and draft→submitted completion rate for search-originated sessions.
- Auth-gate abandonment rate at step 5/6.
- LCP/CLS/INP for the product-detail template.

---

### V2. Homepage first visit → understand value prop → supplier discovery

**Persona & goal.** A lab owner or distributor arrives at `/` from a referral, trade-show contact, or brand search. Goal: understand within one screen what SpexCrafters is (multi-supplier optical B2B marketplace with verification and RFQ), for whom, and how to start — then evaluate suppliers. Corrects the incumbent's "banners + SKU grid, no orientation" homepage.

**Entry points.** Direct/brand search → `/`; referral links; `/about` → `/`.

**Preconditions.** None.

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/` | Lands on homepage | SSR hero states the proposition (who it's for, what it does: discovery, verification, RFQ); primary CTAs "Find suppliers" and "Post an RFQ"; secondary "Sell on SpexCrafters". Category tiles (lenses, frames, machinery, coatings, packaging), verification program explainer, how-it-works strip (search → compare → RFQ → quote → award). No fabricated metrics. |
| 2 | `/` | Clicks "Find suppliers" or a category tile | Navigates to `/suppliers` (directory) or `/marketplace` scoped to category. |
| 3 | `/suppliers` | Browses supplier directory | SSR list with facets: supplier type (lens mfr, frame mfr, lab, coating lab, machinery, packaging, distributor), verification status, country/export markets, certifications (CE/FDA/ISO), MOQ range. Verified suppliers visibly badged; sort defaults disclosed. |
| 4 | `/suppliers` | Filters to verified lens manufacturers exporting to their region | Filter state in URL; result count updates; empty-state offers to relax filters or post an RFQ instead. |
| 5 | `/suppliers/[slug]` | Opens a supplier profile | SSR profile: company overview, capabilities, certifications (issuer, validity where provided), product highlights, verification badge with "what verification checks" link to `/verification`, response-rate/lead-time fields where available (never invented). |
| 6 | `/suppliers/[slug]` | Clicks "Contact supplier", "View products", or "Invite to RFQ" | Products → `/products?supplier=[slug]`. Contact / Invite-to-RFQ → auth gate with `returnTo`; post-auth resumes action (B3 messaging or B5 private RFQ). |
| 7 | `/verification` (optional) | Reads verification program page | Explains document review, badge meaning, and limits of the check — trust content, not marketing. |

**Success criteria.**
- Visitor can articulate the platform model after the first viewport (validated via qualitative testing; proxied by scroll depth and CTA engagement).
- Visitor reaches at least one supplier profile; conversion action initiated (contact, RFQ invite, register) or `/verification` visited (trust intent).

**Failure & edge states.**
- Directory facet combination yields zero suppliers → empty state with "post a public RFQ so suppliers come to you" pathway.
- Supplier profile pending verification → badge area shows "Verification pending" (never a fake badge).
- Suspended supplier → profile returns 404/410 publicly.
- Unsupported locale requested → fallback to `en` with locale switcher visible.

**Notifications/emails.** None (anonymous journey).

**KPIs to instrument.**
- Homepage bounce rate and hero-CTA CTR (find-suppliers vs post-RFQ vs sell-on-SpexCrafters).
- `/` → `/suppliers` → `/suppliers/[slug]` funnel completion.
- Supplier-profile → contact/invite/register action rate.
- `/verification` visit rate from badge taps (trust-content engagement).

---

### V3. Search-driven product discovery with faceted filtering and URL-shareable filter state

**Persona & goal.** A buyer (anonymous or authenticated) needs to narrow thousands of SKUs by deep optical attributes — e.g. single-vision lenses, index 1.61, HMC coating, Rx range to −8.00, MOQ ≤ 500 — and share the exact filtered view with a colleague. Corrects the incumbent's empty facets and non-searchable lens parameters.

**Entry points.** Global search bar (any page); `/marketplace`; `/products`; `/categories/[slug]`; a shared filtered URL.

**Preconditions.** None. Facet schema is category-specific (lenses vs frames vs machinery expose different attribute sets).

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | Any page | Types query in global search, e.g. "1.61 blue cut" | Typeahead suggests products, categories, suppliers, and attribute matches; Enter → `/products?q=1.61+blue+cut`. |
| 2 | `/products?q=…` | Reviews results | SSR results with relevance ranking (PG FTS/trigram), category-scoped facet rail, result count, applied-filter chips. |
| 3 | `/products?…` | Scopes to a category (e.g. Lenses → Single Vision) | Facet rail switches to the category's attribute schema (index, material, design, coating, photochromic, Rx sphere/cylinder range, MOQ, lead time, supplier country, verified-only). URL updates: `/categories/single-vision-lenses?index=1.61&coating=hmc&...`. |
| 4 | `/categories/[slug]?…` | Applies range + multi-select facets | Each change patches the URL query (canonical param order); back/forward restores states; counts per facet value shown; zero-result facets disabled, not hidden. |
| 5 | Same | Sorts (relevance, MOQ, recently added) and pages | Sort/page in URL; pagination SSR-friendly (`?page=n`), `rel=prev/next`-style canonicalization rules applied per SEO policy (only whitelisted facet combinations indexable; the rest `noindex,follow`). |
| 6 | Same | Copies URL and shares with colleague | Colleague opens the URL → identical filter state, sort, and page render server-side; no client state required. |
| 7 | Same | (Authenticated) clicks "Save this search" | Saved search persisted with alert preference (immediate/daily/weekly) → continues in B7. Anonymous users see auth gate with search state preserved. |

**Success criteria.**
- Any filter state is fully reproducible from its URL (idempotent SSR render).
- Filter interactions return results within the search-latency budget; zero-result states always offer recovery (remove last filter, broaden category, post RFQ).
- Facet values reflect real indexed attribute data — no permanently empty facets.

**Failure & edge states.**
- Invalid/unknown query params → ignored gracefully, valid remainder applied, canonical URL cleaned.
- Conflicting range params (min > max) → normalized, user notified via chip state.
- Search backend degraded → fallback to category browse with banner; error logged.
- Very long query strings → capped; server rejects with a usable error page, not a 500.
- Locale switch mid-search → filter state preserved across locale-prefixed URL.

**Notifications/emails.** None in this journey (saved-search alerts covered in B7).

**KPIs to instrument.**
- Search usage rate; zero-result search rate (and top zero-result queries).
- Facet engagement rate per category schema; average filters applied per session.
- Filtered-URL share/open events (referrer-less deep entries onto filtered URLs).
- Search → product-detail CTR; search → RFQ/contact conversion.
- Save-this-search adoption from results.

---

## Buyer journeys

### B1. Buyer registration, email verification, organization creation

**Persona & goal.** A procurement lead creating a SpexCrafters account for their company; goal is a working buyer account bound to a buyer organization so they can contact suppliers and issue RFQs.

**Entry points.** `/auth/register` (direct, or via any auth gate from V1–V3); "Post an RFQ" flows; invite links skip parts of this journey (see S6 for invite mechanics).

**Preconditions.** Email address not already registered (or user chooses login instead).

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/auth/register` | Chooses account intent "I'm buying" (buyer/supplier/both selector) and enters name, work email, password | Validates email format/uniqueness and password policy inline; creates user in `PENDING_VERIFICATION` state; issues single-use, expiring email-verification token. |
| 2 | Email client | Opens "Verify your email" message, clicks link | Token validated (single-use, expiry enforced); user state → `ACTIVE`; session established; resend available with rate limiting. |
| 3 | `/auth/register` (org step) | Chooses "Create a new organization" | Form: legal/company name, org type (retailer, distributor, lab, chain, etc.), country, optional website. Duplicate-name soft warning (suggests requesting to join the existing org instead). |
| 4 | Same | Submits organization | Org created; user assigned `ORG_OWNER` + `BUYER_PROCUREMENT`; org verification status `UNVERIFIED`. |
| 5 | `/buyer/dashboard` | Lands on buyer dashboard | Onboarding checklist rendered (complete profile, verify company, first search, first RFQ); if the user arrived via a gated action (e.g. favorite from V1), that action resumes first via `returnTo`. |

**Success criteria.** Verified user with an owned buyer organization reaches `/buyer/dashboard`; any pre-registration intent (favorite, contact, RFQ draft) is resumed, not lost.

**Failure & edge states.**
- Email already registered → offer login/password-reset; no account enumeration in error copy beyond the standard flow.
- Verification link expired/used → resend flow; tokens invalidated on password change.
- User abandons before org creation → remains "Authenticated User (no organization)": can browse and manage favorites but sees a persistent prompt; RFQ submission and supplier contact require an organization.
- Disposable-email domains → flagged per policy (allow but mark for review, or block, per configuration).
- User belongs to an existing org already (invited earlier) → org-creation step offers "use existing organization" instead.

**Notifications/emails.**
- Email: verification link (step 1); welcome email after verification (locale-aware).
- In-app: onboarding checklist notification on first dashboard visit.

**KPIs to instrument.**
- Registration start → email-submitted → email-verified → org-created funnel (drop-off per step).
- Median time to verify email; resend rate.
- Share of "no organization" accounts older than 7 days.
- Resumed-intent completion rate (gated action finished post-registration).

---

### B2. Buyer onboarding and company verification

**Persona & goal.** The `ORG_OWNER` of a new buyer org completes the company profile and submits business documents so the org earns "Verified buyer" status — increasing supplier response rates and unlocking any verification-gated features (per pricing-visibility policy).

**Entry points.** `/buyer/dashboard` checklist; `/buyer/organization`; `/verification` public explainer CTA.

**Preconditions.** Authenticated; `ORG_OWNER` or `ORG_ADMIN` of a buyer org; org status `UNVERIFIED` or `REJECTED`.

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/buyer/organization` | Opens "Company profile" tab | Shows completeness meter and required fields: legal name, registration country, address, business type, contact. |
| 2 | Same | Completes profile fields | Saved incrementally; completeness meter updates; profile alone does not confer verification. |
| 3 | `/buyer/organization` (verification tab) | Starts verification, reviews required documents list | Requirements shown by registration country (e.g. business registration/license, VAT/tax ID); accepted formats and size limits stated; data-handling notice displayed. |
| 4 | Same | Uploads documents, enters registration numbers | Files validated (type, size, malware scan), stored privately; fields validated for format per country where feasible. |
| 5 | Same | Submits for review | Org verification status → `PENDING_REVIEW`; submission timestamped and immutable snapshot kept for audit; admin queue entry created (A1 governs the review, applied to buyer orgs analogously). |
| 6 | `/buyer/organization` | Awaits/monitors review | Status banner with expected review window (from published SLA config, not invented); user may withdraw and resubmit while pending. |
| 7 | Same | Receives outcome | `VERIFIED`: badge applied org-wide, shown on RFQs and messages to suppliers. `REJECTED`: reasons displayed verbatim from admin review; resubmission opens with prior fields prefilled. |

**Success criteria.** Org reaches `VERIFIED`; badge visible to suppliers on the org's RFQs and messages; rejected orgs can resubmit without re-entering everything.

**Failure & edge states.**
- Upload fails (size/type/scan) → inline error per file; partial submissions saved as draft.
- Documents legible but inconsistent (name mismatch) → admin returns `NEEDS_MORE_INFO` state: targeted request rather than blanket rejection.
- Org details edited after verification (legal name/country) → status downgraded to `REVERIFICATION_REQUIRED` per policy; user warned before saving such edits.
- `ORG_MEMBER` attempts submission → read-only view with "ask your owner/admin" hint.
- Duplicate org detected during review → admin may merge/deny; user notified with support contact.

**Notifications/emails.**
- Email + in-app: submission received; outcome (verified / rejected with reasons / needs more info).
- In-app: reminder if verification draft untouched for N days (configurable).

**KPIs to instrument.**
- Verification start rate among new buyer orgs; start → submit completion rate.
- Median admin review turnaround (also an A1 KPI); first-pass approval rate.
- Rejection-reason distribution; resubmission success rate.
- Supplier response-rate delta for verified vs unverified buyer RFQs (correlational, clearly labeled).

---

### B3. Product discovery → product detail → save favorite → contact supplier

**Persona & goal.** A buyer with an active account evaluates a specific product (e.g. a photochromic 1.56 lens line), saves it for comparison, and opens a product-linked conversation with the supplier.

**Entry points.** V3 search/browse; `/buyer/favorites`; supplier profile product lists; saved-search alert clicks (B7).

**Preconditions.** Authenticated; member of a buyer org (contacting suppliers requires an org context; favoriting requires only authentication).

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/products?…` | Finds candidate product via facets (V3) | Result cards show spec highlights, MOQ, lead time, supplier verification badge. |
| 2 | `/products/[slug]` | Opens product detail | Full spec tables, tier pricing per visibility policy, downloadable documents (spec sheets/certs), supplier card, "similar products" rail. |
| 3 | `/products/[slug]` | Clicks "Save to favorites" | Favorite persisted for the user within the active org context; icon state togglable; optional list/label assignment. |
| 4 | `/products/[slug]` | Clicks "Contact supplier" | Message composer opens with product context attached (product-linked thread); prompts for a concrete first message; template hints (quantities, target market, certifications needed). |
| 5 | Composer modal / `/buyer/messages` | Writes message, optionally attaches file (e.g. Rx range spreadsheet) | Thread created (buyer org ↔ supplier org, product-linked); attachment validated and scanned; message delivered; thread visible in both portals' inboxes. |
| 6 | `/buyer/messages/[threadId]` | Awaits reply; continues conversation | Read receipts and reply notifications per X4; supplier reply arrives with in-app + email notification. |
| 7 | `/buyer/favorites` | Later returns via favorites | Favorites list with spec-highlight columns, change indicators (price tier updated, product unpublished), jump-off to compare or start RFQ prefilled with a favorited product. |

**Success criteria.** Favorite persists across sessions and org context; a product-linked thread exists with the first supplier reply; buyer can navigate favorites → RFQ creation without re-entering specs.

**Failure & edge states.**
- User has no org yet → contact CTA routes through org creation (B1 step 3) with intent preserved.
- Supplier has messaging disabled toward unverified buyers (supplier preference) → CTA explains and points to B2 verification.
- Product unpublished after favoriting → favorite retained with "no longer listed" state; suggests alternatives.
- Attachment rejected (type/size/malware) → inline error; message not lost.
- Supplier org suspended mid-thread → thread locked read-only with system notice (X4).

**Notifications/emails.**
- Supplier: in-app + email "New product inquiry" with product context.
- Buyer: in-app + email on supplier reply (digest rules per X4).
- Buyer: optional in-app alert when a favorited product materially changes (price tiers, availability), governed by notification settings.

**KPIs to instrument.**
- Favorite adds per active buyer; favorites → RFQ/contact conversion rate.
- Product-detail → contact-supplier rate; first-message → supplier-reply rate and median reply latency.
- Thread abandonment rate (no buyer follow-up after supplier reply).

---

### B4. Create & publish a public RFQ with specs and file uploads

**Persona & goal.** A buyer needs 5,000 units of a spec'd item (e.g. TR-90 frames, custom colorway) and publishes a public RFQ so relevant suppliers can quote. Fills the incumbent's "no path for the 5,000-unit buyer" gap.

**Entry points.** `/rfq/new` (public CTA, homepage, product/category pages with prefill); `/buyer/rfqs` → "New RFQ"; `/rfq` explainer page.

**Preconditions.** Submission requires authenticated user with `BUYER_PROCUREMENT` in a buyer org (drafting may begin anonymously per V1). Org not suspended.

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/rfq/new` | Selects category (lenses / frames / machinery / packaging / other) | Loads category-specific spec form (same attribute schema as catalog facets: e.g. lens index, coating, Rx range; frame material, dimensions; machinery type/power). |
| 2 | `/rfq/new` | Completes structured specs + free-text description | Inline validation; autosave to server-side draft; required vs optional fields marked; quantity, target unit price (optional), destination market, required certifications. |
| 3 | `/rfq/new` | Sets commercial terms | Quantity/MOQ expectations, Incoterms preference, delivery destination, target date, quote deadline, currency; deadline validated (min/max window). |
| 4 | `/rfq/new` | Uploads files (tech packs, drawings, reference images) | Files validated/scanned, attached to draft; per-file and total-size limits enforced. |
| 5 | `/rfq/new` | Chooses visibility "Public" and reviews summary | Preview of the RFQ as suppliers will see it; buyer identity display rule shown (org name + verification badge; contact details withheld until quoting/messaging per policy). |
| 6 | `/rfq/new` | Publishes | RFQ status → `OPEN` (or `PENDING_REVIEW` if moderation rules trigger — new org, flagged keywords; see A3). Listed in the supplier RFQ marketplace; matching engine notifies suppliers whose categories/capabilities match. |
| 7 | `/buyer/rfqs/[id]` | Monitors RFQ | Dashboard shows views, invited/quoting suppliers count, quotes received, time to deadline; buyer can edit non-material fields, extend deadline once (audit-logged), or close early. |

**Success criteria.** RFQ visible in the supplier marketplace with complete structured specs; at least one quotation received before deadline (activation event); buyer can manage lifecycle from `/buyer/rfqs`.

**Failure & edge states.**
- Draft abandoned → retained with TTL; resumption prompt on return.
- Moderation hold (A3) → buyer sees `PENDING_REVIEW` status with expected window; publish notification deferred until approval.
- Zero quotes at deadline → RFQ auto-`EXPIRED`; buyer prompted to relax specs, extend, or convert to private invitations (B5).
- Material edit after quotes exist → blocked; buyer must close and reissue (protects quote integrity), non-material clarifications allowed and appended as timestamped addenda visible to all quoters.
- Unverified org publishing high-volume RFQs → rate limits and verification nudge.
- File upload exceeds limits → clear per-file error, draft intact.

**Notifications/emails.**
- Buyer: publish confirmation (in-app + email); quote-received notifications; deadline-approaching reminder; expiry summary.
- Suppliers: matched-RFQ notification per their alert settings (S4).
- Admin: moderation-queue entry only when rules trigger.

**KPIs to instrument.**
- RFQ funnel: form start → draft → publish (drop-off per step, per entry point).
- Time-to-first-quote; quotes per RFQ; % RFQs with ≥1 / ≥3 quotes at deadline.
- Moderation-hold rate and hold duration.
- Spec completeness score vs quote count (form-quality feedback loop).

---

### B5. Private RFQ to selected suppliers

**Persona & goal.** A buyer with sensitive requirements (private-label program, confidential pricing) invites 3–5 shortlisted suppliers instead of publishing publicly.

**Entry points.** `/rfq/new` with visibility "Private"; "Invite to RFQ" from `/suppliers/[slug]` or from favorites/threads; reissue of an expired public RFQ (B4 edge).

**Preconditions.** As B4; buyer has identified at least one target supplier (directory, favorites, or prior threads).

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/rfq/new` | Builds RFQ as in B4 steps 1–4, selects visibility "Private" | Supplier-invitation panel replaces public preview; explains that only invited suppliers can see the RFQ. |
| 2 | `/rfq/new` (invite panel) | Searches/selects suppliers (typeahead over directory; quick-add from favorites and message history) | Selection list with verification badges and capability match hints; min 1, max N invitations (policy-configurable). |
| 3 | `/rfq/new` | Publishes privately | RFQ status `OPEN` with `visibility=PRIVATE`; invitation records created per supplier (`INVITED`); RFQ excluded from the public marketplace and search. |
| 4 | Supplier portal | Invited suppliers respond | Each invitation moves through `VIEWED` → `QUOTED` / `DECLINED` (with optional decline reason: capacity, spec mismatch, MOQ). Declines are visible to the buyer immediately. |
| 5 | `/buyer/rfqs/[id]` | Monitors invitation grid | Per-supplier status matrix (invited/viewed/quoted/declined); buyer may invite additional suppliers while open; cannot convert to public without reissuing (identity/expectation integrity). |
| 6 | `/buyer/rfqs/[id]` | Proceeds to comparison | Continues in B6 once quotes arrive. |

**Success criteria.** All invitations delivered; buyer can see per-supplier engagement; ≥1 quotation received; RFQ never leaks to non-invited suppliers (including via search, sitemaps, or notification content).

**Failure & edge states.**
- Invited supplier suspended before viewing → invitation auto-revoked, buyer notified with suggestion to replace.
- All invitees decline → buyer prompted to add invitees, adjust specs, or republish as public.
- Supplier attempts access after invitation revoked/RFQ closed → 403 with neutral message (no spec leakage).
- Invitee org has messaging blocked with the buyer org (X4 block state) → invitation blocked with explanation to buyer.
- Buyer invites a supplier that cannot serve the destination market (declared export markets) → soft warning, not a block.

**Notifications/emails.**
- Suppliers: invitation (in-app + email) with category and deadline (spec details behind auth); reminder before deadline if `INVITED`/`VIEWED` but not quoted.
- Buyer: per-supplier viewed/quoted/declined events (batched); deadline summary.

**KPIs to instrument.**
- Invitations per private RFQ; invitation → viewed → quoted conversion per stage.
- Decline rate and reason distribution.
- Private vs public RFQ quote latency and award rate (comparative).

---

### B6. Receive quotations → compare → negotiate via messaging → award → close

**Persona & goal.** The buyer evaluates competing quotations on price, MOQ, lead time, Incoterms, and payment terms, negotiates with front-runners, awards one supplier, and closes the RFQ cleanly for the rest.

**Entry points.** Quote-received notification → `/buyer/rfqs/[id]`; `/buyer/quotations`; `/buyer/dashboard` widgets.

**Preconditions.** RFQ `OPEN` with ≥1 quotation in `SUBMITTED` (or revised) state; user has `BUYER_PROCUREMENT` in the owning org.

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/buyer/rfqs/[id]` | Opens quotations tab | Quotation list: supplier (with verification badge), unit price, currency, MOQ, lead time, Incoterms, payment terms, validity expiry, revision count, attachments. |
| 2 | Same (compare view) | Selects 2–5 quotations → "Compare" | Side-by-side matrix normalizing fields; display-currency conversion clearly labeled with rate/date; deltas highlighted; validity countdowns shown. |
| 3 | `/buyer/messages/[threadId]` | Opens RFQ-linked thread with a front-runner; negotiates (e.g. asks for better price at higher volume) | Thread is RFQ-linked and quotation-aware; supplier may respond with a revision (S5), which appears as a new quotation version; version history preserved and diffable. |
| 4 | `/buyer/rfqs/[id]` | Reviews revised quotation(s) | Comparison matrix updates to latest versions; superseded versions accessible in history; expired quotations flagged and excluded from award by default. |
| 5 | Same | Clicks "Award" on the chosen quotation | Confirmation dialog restates terms snapshot; on confirm: quotation → `AWARDED`; RFQ → `AWARDED`; a terms snapshot document (immutable) is generated and attached to the thread for both parties. |
| 6 | Same | Non-awarded handling | All other active quotations → `NOT_AWARDED`; their suppliers notified with neutral copy (no competitor details); buyer may optionally send a templated thank-you note. |
| 7 | `/buyer/rfqs/[id]` | Closes out | RFQ → `CLOSED` after award (auto); awarded thread remains open for fulfillment coordination (ordering/fulfillment is off-platform in v1; the thread and snapshot are the handoff record). Buyer prompted for an optional supplier experience rating per policy. |

**Success criteria.** Award recorded with an immutable terms snapshot; all parties notified with correct, minimal information; comparison decisions were made on like-for-like normalized data; no state where two quotations are simultaneously `AWARDED`.

**Failure & edge states.**
- Quotation validity expires mid-comparison → state `EXPIRED`; buyer can request revalidation via thread (supplier re-issues, S5).
- Supplier withdraws a quotation pre-award (S5) → removed from active comparison, kept in history; buyer notified.
- Award attempted on expired/withdrawn quotation → blocked with explanation.
- Concurrent award clicks (two org members) → optimistic-locking; second actor sees "already awarded".
- Buyer closes RFQ without award → status `CLOSED_NO_AWARD`; suppliers notified neutrally; reasons optional.
- Awarded supplier goes unresponsive post-award → thread nudge tooling and support escalation path (PLATFORM_SUPPORT visibility); award state unchanged (no forced un-award in v1; documented support process instead).

**Notifications/emails.**
- Buyer: new quotation, quotation revised, quotation withdrawn, validity-expiring (for shortlisted), award confirmation with snapshot link.
- Awarded supplier: award notification (in-app + email) with snapshot link.
- Non-awarded suppliers: neutral outcome notification.

**KPIs to instrument.**
- Compare-view usage rate before award; quotations compared per award.
- Negotiation depth (messages + revisions per awarded quotation); revision → award lift.
- RFQ publish → award cycle time; award rate per RFQ cohort (public vs private).
- `CLOSED_NO_AWARD` rate and stated reasons.

---

### B7. Saved search creates alert → notification → return visit

**Persona & goal.** A buyer tracking a supply need over time (e.g. "verified suppliers, 1.67 MR-7 lenses, MOQ ≤ 300") saves the search and returns when new matches appear — the platform's re-engagement loop.

**Entry points.** "Save this search" on `/products`/`/categories/[slug]` results (V3 step 7); `/buyer/saved-searches`; alert email/notification click.

**Preconditions.** Authenticated (saved searches are user-scoped within org context); at least one search executed.

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/products?…` | Clicks "Save this search" | Dialog: name (prefilled from filters), alert frequency (immediate / daily digest / weekly digest / none), channel (in-app, email). Persists the canonical filter state, not the raw URL string. |
| 2 | `/buyer/saved-searches` | Reviews saved searches | List with human-readable filter summary, frequency, last-run/last-match times, new-match badge; edit/pause/delete inline. |
| 3 | (Background) | — | Matching job evaluates new/updated published products against saved-search criteria; matches deduplicated per search per item; digests assembled per frequency in user locale/timezone. |
| 4 | Email / in-app | Receives "3 new matches for 'MR-7 ≤300 MOQ'" | Email lists top matches with spec highlights; single CTA deep-links to the saved search results view filtered to new-since-last-visit. |
| 5 | `/products?…&saved=[id]` | Clicks through | SSR results with "new" badges on unseen items; last-seen watermark updated; from here the buyer proceeds to B3 (favorite/contact) or B4 (RFQ). |
| 6 | `/buyer/saved-searches` | Adjusts frequency or pauses | Preference updated immediately; unsubscribe link in every alert email maps to the specific search's settings (one-click, no login required for pause, per email-compliance policy). |

**Success criteria.** Alerts contain only genuinely new/changed matches (no repeats); click-through lands on an exact reproducible result state; unsubscribe/pause honored immediately.

**Failure & edge states.**
- Saved search's category/attribute schema changes (facet renamed/retired) → search auto-migrated where mappable, otherwise flagged "needs attention" with edit prompt; alerts suspended until fixed.
- Zero matches for a long period → periodic "still watching" note per policy or silent (configurable); never a fabricated match.
- User leaves the org → org-context searches handled per data policy (transferred or deleted; defined in org offboarding).
- Email bounces repeatedly → channel auto-disabled, in-app notice shown.
- Matching job backlog → alerts late but never duplicated (idempotency keys per search × item).

**Notifications/emails.**
- Email + in-app: match alerts per chosen frequency; digest respects locale and timezone.
- In-app: "needs attention" state changes.

**KPIs to instrument.**
- Saved-search creation rate per active buyer; searches per user.
- Alert open rate, CTR, and alert → session conversion (return-visit driver).
- Post-alert action rate (favorite/contact/RFQ within the alert session).
- Pause/unsubscribe rate per frequency (alert-fatigue signal).

---

## Supplier journeys

### S1. Supplier registration, organization creation, onboarding wizard

**Persona & goal.** A sales director at a lens manufacturer registers, creates the supplier organization, and completes the onboarding wizard (company profile, capabilities, certifications, documents) to become discoverable.

**Entry points.** `/auth/register` with intent "I'm selling"; "Sell on SpexCrafters" CTAs (`/`, `/pricing`); direct `/supplier/onboarding` (redirects to register if anonymous).

**Preconditions.** Email not already registered; company not already on the platform (duplicate handling below).

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/auth/register` | Registers with intent "supplier" (as B1 steps 1–2) | User created and email-verified; intent stored to route org creation into the supplier path. |
| 2 | `/auth/register` (org step) | Creates supplier organization: company name, supplier type(s) (lens mfr / frame mfr / lab / coating lab / machinery / packaging / distributor), country | Org created with `supplier` capability; user assigned `ORG_OWNER` + `SUPPLIER_SALES` + `SUPPLIER_CATALOG`; duplicate-name check suggests join-request instead of duplicate creation. |
| 3 | `/supplier/onboarding` | Wizard step: company profile | Legal details, address, founding year, employee band, factory locations, export markets, languages; autosaved per step; progress indicator across all steps. |
| 4 | `/supplier/onboarding` | Wizard step: capabilities | Category-specific capability forms (e.g. lens: indices produced, coating lines, Rx lab services; frames: materials, processes like acetate/injection/titanium; machinery: equipment classes). These feed directory facets and RFQ matching. |
| 5 | `/supplier/onboarding` | Wizard step: certifications & documents | Uploads certificates (CE/FDA/ISO etc.) with issuer/number/expiry metadata; business documents staged for verification (S2); files validated/scanned. |
| 6 | `/supplier/onboarding` | Wizard step: public profile preview | Renders the future `/suppliers/[slug]` page from entered data; flags gaps that reduce discoverability (no logo, no capabilities). |
| 7 | `/supplier/onboarding` | Finishes wizard | Profile saved; public listing state per policy: minimal profile may go live unverified (badge absent) or held until verification — policy-flagged, default: live-unverified with clear "Unverified" label. Dashboard checklist now points to S2 (verify) and S3 (first product). |

**Success criteria.** Supplier org exists with capabilities captured in structured form (usable by matching and facets); wizard completed or resumable at the exact step left; clear next actions (verification, first product).

**Failure & edge states.**
- Wizard abandoned mid-way → resume banner on every `/supplier/*` visit; incomplete profiles excluded from directory facets that require the missing data.
- Duplicate company detected (name/registration number) → join-request flow to the existing org's owner instead of a second org; conflicts escalated to support.
- Certificate file present but metadata missing → saved as draft, excluded from public display until complete.
- User registered with buyer intent but wants to sell too → org can hold both capabilities; wizard reachable from org settings (no second account needed).
- Unsupported supplier type → "other" with free text, queued for taxonomy review.

**Notifications/emails.**
- Email: verification link, welcome-supplier email with onboarding checklist.
- In-app: wizard-resume reminders (capped); "profile live" confirmation.

**KPIs to instrument.**
- Registration → org-created → wizard-completed funnel; drop-off per wizard step.
- Median time to complete onboarding; profile completeness score distribution.
- Share of suppliers reaching "live profile" within 7 days.

---

### S2. Verification submission → admin review → verified badge

**Persona & goal.** The supplier `ORG_OWNER`/`ORG_ADMIN` submits business documents for the verification program; goal is the "Verified supplier" badge — the platform's core trust differentiator.

**Entry points.** Onboarding wizard hand-off (S1 step 5); `/supplier/company` verification tab; dashboard checklist; `/verification` explainer.

**Preconditions.** Supplier org exists; company profile minimally complete; status `UNVERIFIED`, `REJECTED`, or `NEEDS_MORE_INFO`.

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/supplier/company` (verification tab) | Reviews requirements | Country-specific document checklist (business license/registration, tax ID, factory evidence per policy); what reviewers check and what the badge does/doesn't assert; data-handling notice. |
| 2 | Same | Uploads documents, enters registration data | Validation, malware scan, private storage; per-document status chips (uploaded/needs replacement). |
| 3 | Same | Submits | Status → `PENDING_REVIEW`; immutable submission snapshot; entry appears in `/admin/verification` queue (A1); submission is version-numbered for resubmissions. |
| 4 | Same | Monitors status | Banner with queue-state and published SLA window; withdraw-and-edit allowed while pending (returns to draft). |
| 5 | — | Admin reviews (A1) | Outcome recorded with reasons and audit log. |
| 6 | `/supplier/company` | Receives outcome | `VERIFIED`: badge on `/suppliers/[slug]`, product cards, quotations, and messages; verification date shown publicly. `NEEDS_MORE_INFO`: targeted list of items, submission reopens partially. `REJECTED`: reasons shown; cool-down before resubmission per policy. |
| 7 | (Ongoing) | Maintains status | Certificate/document expiry tracked; expiry approaching → renewal task; expired critical document → badge suspended per policy with grace period, not silent removal. |

**Success criteria.** Badge accurately reflects review state everywhere it renders (profile, products, quotes, messages, directory facets); supplier always knows the current state and the exact next action.

**Failure & edge states.**
- Illegible/cropped documents → `NEEDS_MORE_INFO` with per-document annotation from reviewer.
- Legal-entity mismatch between profile and documents → reviewer flags; org must correct profile (which may require re-review of other fields).
- Fraudulent documents suspected → escalated to `PLATFORM_ADMIN`; org may be suspended (A3); audit trail preserved.
- Org edits legal identity post-verification → `REVERIFICATION_REQUIRED` (as B2).
- Reviewer requests info while supplier is mid-edit → optimistic concurrency; supplier sees refreshed state before submitting.

**Notifications/emails.**
- Email + in-app: submission received; outcome (verified / needs-more-info with items / rejected with reasons); document-expiry warnings (T-30/T-7 style, config-driven); badge-suspension notice.

**KPIs to instrument.**
- Submission rate among onboarded suppliers; time from org creation to submission.
- Review turnaround (queue-entry → decision); first-pass approval rate; needs-more-info loop count.
- Badge coverage: % of directory-listed suppliers verified.
- Buyer engagement delta on verified vs unverified supplier profiles (correlational, labeled).

---

### S3. Product creation with category-specific technical attributes and media

**Persona & goal.** A catalog manager (`SUPPLIER_CATALOG`) publishes a product with deep, structured optical specs and media so it is facetable, comparable, and SEO-indexable.

**Entry points.** `/supplier/products` → "New product"; dashboard checklist; bulk-import path (out of scope here; single-product flow below).

**Preconditions.** Supplier org exists; user has `SUPPLIER_CATALOG`; org not suspended. Publication visibility may depend on moderation policy (A2).

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/supplier/products/new` | Selects category (drives the schema) | Loads category-specific attribute form: lenses (index, material, design, coating stack, photochromic, UV, Rx sphere/cylinder ranges, diameter, Abbe), frames (material, hinge type, lens/bridge/temple dimensions, weight, sizes/colors), machinery (type, capacity, power, dimensions, compliance marks), packaging, etc. |
| 2 | Same | Enters identity & commercial data | Human-readable product name (required — no bare SKU codes as names, correcting the incumbent), internal SKU, description, MOQ, lead-time range, price tiers with currency, sample availability, customization options (e.g. logo engraving). |
| 3 | Same | Completes technical attributes | Required attributes per category enforced; units normalized (mm, g, diopters); ranges validated (e.g. SPH min ≤ max); attribute data feeds facets (V3) and RFQ matching. |
| 4 | Same | Uploads media & documents | Images validated (min resolution, formats), reordered by drag; spec-sheet/cert PDFs attached; alt text prompted; image pipeline generates responsive variants. |
| 5 | Same | Previews | Renders the public `/products/[slug]` view including SEO snippet preview (title/meta) and completeness score. |
| 6 | Same | Publishes | Status → `PUBLISHED` immediately, or `PENDING_REVIEW` when moderation rules apply (new supplier, flagged content — A2). Slug generated (uniqueness enforced, history kept for 301s); product enters search index on publish. |
| 7 | `/supplier/products` | Manages catalog | List with status, completeness, views/saves counters (from analytics), edit/unpublish/duplicate actions; edits to published products follow the same validation; material spec changes are versioned. |

**Success criteria.** Product is fully facetable (all required structured attributes present), renders an indexable SSR page with unique metadata, and appears in relevant search/facets shortly after publish.

**Failure & edge states.**
- Missing required attributes → cannot publish; can save as `DRAFT` indefinitely.
- Image below quality threshold → warning (soft) or block (hard) per rule tier.
- Duplicate-looking product (same supplier, near-identical attributes) → soft duplicate warning.
- Moderation rejection (A2) → status `REJECTED` with reasons; edit-and-resubmit loop.
- Category schema versioned after publish → product flagged for attribute migration; remains live with legacy attributes until migrated.
- Supplier loses verified badge → products remain live but badge display updates everywhere (no stale trust signals).

**Notifications/emails.**
- In-app: publish confirmation; moderation outcome (approved/rejected with reasons); attribute-migration tasks.
- Email: moderation rejection (actionable), digest of catalog issues (optional).

**KPIs to instrument.**
- Product-creation funnel: start → draft → publish; median time to first published product per new supplier.
- Attribute completeness score at publish; % products passing moderation first time.
- Published products per supplier; product-page impressions/saves/inquiries per product (supplied back via `/supplier/analytics`).

---

### S4. RFQ discovery (browse/filter/matched) → submit quotation

**Persona & goal.** A supplier sales rep (`SUPPLIER_SALES`) finds relevant open RFQs — via browsing, filtering, or match notifications — and submits a competitive quotation.

**Entry points.** `/supplier/rfqs` (marketplace tab: public RFQs matching capabilities; invitations tab: private invitations from B5); matched-RFQ notification/email; dashboard widget.

**Preconditions.** Supplier org active; user has `SUPPLIER_SALES`; org capabilities/categories set (drives matching quality). Quoting may require verified status per policy (flagged; default: unverified may quote, badge shown either way).

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/supplier/rfqs` | Opens RFQ marketplace | Default view filtered to org's declared categories/capabilities; facets: category, destination market, quantity band, deadline, buyer-verified-only; sort by newest/deadline/match score. Private invitations surfaced in a distinct tab with status chips. |
| 2 | `/supplier/rfqs/[id]` | Opens an RFQ | Full structured specs, attachments, commercial terms, buyer org name + verification badge, deadline countdown, count of quotations received (count only — no competitor pricing visibility). Viewing a private invitation marks it `VIEWED` (B5). |
| 3 | Same | Clicks "Submit quotation" | Quotation form: unit price + currency, MOQ, quantity breaks (optional tier rows), lead time, Incoterms, payment terms, validity period (required, bounded), notes, attachments (spec confirmations, alternatives). |
| 4 | Same | Completes and reviews | Validation (validity within RFQ deadline+policy window; currency supported); summary preview exactly as the buyer will see it. |
| 5 | Same | Submits | Quotation status `SUBMITTED` (version 1); RFQ-linked thread auto-created or attached; buyer notified (B6); quotation locked to the submitting org with editable-via-revision semantics (S5). |
| 6 | `/supplier/quotations` | Tracks quotations | Pipeline list by status (`SUBMITTED` / `REVISED` / `AWARDED` / `NOT_AWARDED` / `EXPIRED` / `WITHDRAWN`), validity countdowns, buyer activity indicators (viewed/compared per privacy policy), links into threads for negotiation (S5/B6). |

**Success criteria.** Relevant RFQs are discoverable within the supplier's capability scope; quotation submitted with complete commercial terms; supplier can track state changes without polling the buyer.

**Failure & edge states.**
- RFQ deadline passes mid-composition → submission blocked with clear message; draft retained in case buyer extends.
- RFQ closed/awarded while drafting → same handling; notification explains.
- Supplier outside declared capabilities quotes anyway → allowed (declared capabilities are matching hints, not hard gates) unless policy says otherwise; flagged in buyer view as "outside declared capabilities" only if policy enables it.
- Duplicate quotation attempt on same RFQ → blocked; edit/revise the existing one instead.
- Validity period set shorter than buyer's comparison window → warning nudge.
- Suspended buyer org → RFQ delisted; existing drafts frozen with notice.

**Notifications/emails.**
- Supplier: matched-RFQ alerts (frequency-controlled), invitation notifications and deadline reminders (B5), quotation state changes (awarded/not-awarded/expired), buyer messages.
- Buyer: quotation-received (B6).

**KPIs to instrument.**
- Match-notification CTR; RFQ-view → quotation-start → submit funnel.
- Quotations per active supplier per month; median time from RFQ publish to supplier's quote.
- Win rate per supplier segment (verified vs not, matched vs browsed origin).
- Draft-loss rate due to deadline/closure (form-timing signal).

---

### S5. Quotation revision after buyer negotiation; withdrawal

**Persona & goal.** After buyer pushback (B6 step 3), the supplier revises price/terms — or withdraws when they can no longer honor the quote. Goal: negotiate without losing the audit trail of what was offered when.

**Entry points.** Buyer message in RFQ-linked thread → `/supplier/messages/[threadId]`; `/supplier/quotations/[id]`; validity-expiring reminder.

**Preconditions.** Quotation exists in `SUBMITTED`/`REVISED` state; RFQ still `OPEN`; user has `SUPPLIER_SALES` in the owning org.

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/supplier/messages/[threadId]` | Reads buyer's negotiation message ("target $2.10 at 10k units") | Thread shows quotation context card (current version, validity); quick action "Revise quotation". |
| 2 | `/supplier/quotations/[id]/revise` | Opens revision form prefilled with current version | All fields editable; changes diffed against current version live; new validity period required. |
| 3 | Same | Submits revision | New immutable version created (v2, v3, …); status `REVISED`; prior versions preserved and visible to both parties in history; buyer's comparison view updates to latest (B6 step 4); revision event posted into the thread automatically. |
| 4 | `/supplier/quotations/[id]` | (Alternative) clicks "Withdraw" | Confirmation with reason selector (capacity, cost change, error, other + free text, reason sharing per policy); status → `WITHDRAWN` (terminal); removed from buyer's active comparison, retained in history; thread remains open. |
| 5 | `/supplier/quotations/[id]` | (Alternative) validity lapses | Auto-transition to `EXPIRED`; supplier may re-issue as a new version if RFQ still open ("revalidate" quick action). |

**Success criteria.** Every version immutable and timestamped; the buyer always compares the latest version; withdrawal is unambiguous and cannot be confused with expiry; no revision possible after award or RFQ close.

**Failure & edge states.**
- Revision attempted after RFQ closed/awarded → blocked with state explanation.
- Award happens while a revision draft is open → draft invalidated with notice (optimistic concurrency on RFQ state).
- Excessive revision churn (> N versions) → soft rate limit and nudge toward thread negotiation before re-quoting.
- Withdrawal of an `AWARDED` quotation → not possible in-product; routed to support (contractual implications are off-platform in v1).
- Two org members revising simultaneously → last-writer conflict surfaced; versions never silently merged.

**Notifications/emails.**
- Buyer: quotation revised (with diff summary), quotation withdrawn, quotation expired.
- Supplier: validity-expiring reminder (before expiry), award/close events that block revision.

**KPIs to instrument.**
- Revisions per quotation; revision latency after buyer negotiation message.
- Withdrawal rate and reason distribution; expiry rate (validity hygiene).
- Revised-quotation award rate vs single-version award rate.

---

### S6. Team management: invite employee, assign roles

**Persona & goal.** A supplier `ORG_OWNER` invites a colleague (e.g. a catalog specialist) and scopes their access with capability roles. The same mechanics apply to buyer orgs at `/buyer/organization` (members tab).

**Entry points.** `/supplier/team`; dashboard checklist ("add your team"); settings.

**Preconditions.** User is `ORG_OWNER` or `ORG_ADMIN`; org active; seat limits per plan (`/pricing`) not exceeded.

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/supplier/team` | Opens team page | Member list: name, email, org role (`ORG_OWNER`/`ORG_ADMIN`/`ORG_MEMBER`), capability roles (`SUPPLIER_SALES`, `SUPPLIER_CATALOG`), status (active/invited/deactivated), last active. |
| 2 | Same | Clicks "Invite member", enters email, selects org role + capability roles | Role matrix with plain-language descriptions of what each grants; `ORG_OWNER` cannot be granted via invite (transfer is a separate explicit flow); invitation record created with expiring token. |
| 3 | Invitee's email | Opens invitation email, clicks accept link | Existing SpexCrafters user → org membership added after confirmation (multi-org supported, X3). New user → registration (B1 steps 1–2) with org join replacing org creation; roles applied as invited. |
| 4 | `/supplier/team` | Inviter monitors | Invitation states: `PENDING` / `ACCEPTED` / `EXPIRED` / `REVOKED`; resend and revoke actions; audit log records who invited/changed whom. |
| 5 | Same | Adjusts roles later | Role changes take effect immediately (session claims refreshed); demoting the last `ORG_OWNER` is blocked; removing a member revokes portal access but preserves their historical artifacts (quotes, messages) attributed to them. |
| 6 | Same | Deactivates a departed employee | Membership deactivated; active sessions invalidated; open work surfaced for reassignment (assigned RFQs/threads). |

**Success criteria.** Invitee reaches the correct portal with exactly the invited permissions; no orphaned org (always ≥1 owner); every membership/role change is audit-logged.

**Failure & edge states.**
- Invitation to an email already in the org → blocked with pointer to existing member.
- Token expired → inviter can resend; old token invalid.
- Invitee accepts after being revoked → friendly "invitation no longer valid".
- Seat limit reached → upsell path to `/pricing` without losing the drafted invite.
- Invitee already owns another org (multi-org) → accept flow confirms which org context they are joining; X3 governs switching.
- Member with in-flight quotations removed → quotations remain valid (org-owned), reassignment prompt for thread ownership.

**Notifications/emails.**
- Invitee: invitation email (locale of inviter's org default, switchable); acceptance confirmation.
- Inviter: acceptance/expiry notifications.
- All admins: audit-relevant notices per policy (e.g. owner transfer).

**KPIs to instrument.**
- Invitations sent per org; acceptance rate and median time-to-accept.
- Multi-member org share (collaboration adoption); role distribution.
- Deactivation → reassignment completion rate.

---

## Admin journeys

### A1. Supplier verification review (approve/reject with reasons, audit log)

**Persona & goal.** A `PLATFORM_ADMIN` (or trained `PLATFORM_MODERATOR` where policy allows) works the verification queue: examine submitted documents, decide, and record an auditable outcome. Integrity of this process is the platform's trust product.

**Entry points.** `/admin/verification` queue; escalation links from support tickets; `PLATFORM_AUDITOR` accesses the audit trail read-only.

**Preconditions.** Platform role with verification-review permission; submission in `PENDING_REVIEW`. Reviewers cannot review orgs they are personally associated with (conflict rule).

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/admin/verification` | Opens queue | Sortable queue: submission age, org type, country, resubmission count, SLA breach flags; claims model prevents two reviewers processing one submission (assignment lock). |
| 2 | `/admin/verification/[id]` | Claims a submission | Locked to reviewer; full view: org profile, submitted documents (inline viewer), registration numbers, prior submission history and decisions, cross-checks (duplicate registration numbers across orgs). |
| 3 | Same | Verifies documents against checklist | Per-item checklist recorded (document legible, entity name matches, registry lookup done where applicable); notes field; every view/download of documents is audit-logged. |
| 4 | Same | Decides: Approve / Needs more info / Reject | Approve → org `VERIFIED`, badge propagates (S2/B2). Needs-more-info → itemized requests sent to org. Reject → structured reason codes + free-text (buyer/supplier-visible text is the reviewer-written reason; internal notes stay internal). |
| 5 | Same | Confirms decision | Immutable audit record: reviewer, timestamps, checklist state, decision, reasons, document versions referenced. Org notified (S2 step 6 / B2 step 7). Queue advances. |
| 6 | `/admin/verification` (audit tab) | (`PLATFORM_AUDITOR`) reviews decisions | Read-only trail with filters (reviewer, outcome, period); export per compliance policy. |

**Success criteria.** Every decision has a complete audit record; SLA-tracked turnaround; decisions are consistent (reason codes enable QA sampling); no submission reviewable by a conflicted or unassigned reviewer.

**Failure & edge states.**
- Suspected fraud → escalate to `SUPER_ADMIN`/`PLATFORM_ADMIN` with org-suspension option (A3); submission frozen.
- Reviewer abandons a claimed item → lock auto-expires back to queue after timeout.
- Org withdraws submission mid-review → review closed `WITHDRAWN_BY_ORG`, noted in history.
- External registry unavailable → decision may be deferred (`ON_HOLD` with reason) rather than guessed.
- Appeal of rejection → resubmission with incremented version; prior decision immutable.

**Notifications/emails.**
- Org: outcome notifications (as S2/B2).
- Reviewers: SLA-breach and queue-depth alerts; escalation assignments.

**KPIs to instrument.**
- Queue depth and age distribution; SLA compliance rate.
- Decisions per reviewer per day; inter-reviewer consistency (QA sample agreement).
- Needs-more-info loop rate; escalation/fraud-flag rate.

---

### A2. Product moderation queue

**Persona & goal.** A `PLATFORM_MODERATOR` reviews products flagged into moderation (new-supplier holds, keyword/image rules, abuse reports) to keep catalog quality and legality high without throttling legitimate publishing.

**Entry points.** `/admin/moderation/products`; abuse-report links (A3); automated-rule flags.

**Preconditions.** `PLATFORM_MODERATOR` or above; items in `PENDING_REVIEW` or `FLAGGED` state.

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/admin/moderation/products` | Opens queue | Items with flag source (new-supplier hold, rule match, user report), rule details, supplier context (verification status, prior violations); claim/assignment lock as A1. |
| 2 | `/admin/moderation/products/[id]` | Reviews product | Full product view as buyers would see it + raw attribute data + media originals; flag rationale highlighted (matched keyword, reported reason); supplier's moderation history sidebar. |
| 3 | Same | Decides: Approve / Reject / Request changes | Approve → `PUBLISHED` (indexed). Reject → `REJECTED` with reason codes + supplier-visible text (S3 loop). Request changes → `CHANGES_REQUESTED` with itemized list; product stays unpublished. |
| 4 | Same | (When warranted) applies supplier-level action | Repeat violations → recommend account action (A3 handles suspension); strike recorded on the org's moderation ledger. |
| 5 | `/admin/moderation/products` | Continues queue | Decision audit-logged (moderator, reasons, timestamps); false-positive marks feed rule tuning. |

**Success criteria.** Median moderation latency within published SLA; decisions actionable for suppliers (specific reasons); rule false-positive rate tracked and declining; no approved item violating listing policy on re-audit samples.

**Failure & edge states.**
- Borderline IP/counterfeit concern → `ON_HOLD` + escalate to `PLATFORM_ADMIN`; supplier notified of extended review without accusation language.
- Product edited while under review → review resets to the edited version (single source of truth), moderator notified.
- Mass rule misfire (e.g. keyword too broad floods queue) → bulk-release tooling for `PLATFORM_ADMIN` with audit note.
- Supplier resubmits without changes after rejection → auto-flagged with prior decision attached.
- Reported product already unpublished by supplier → report closed `MOOT`, reporter notified generically.

**Notifications/emails.**
- Supplier: approval, rejection with reasons, changes-requested (in-app + email).
- Moderators: queue-depth/SLA alerts.
- Reporter (if a user report initiated it): closure notice without decision detail (A3 pattern).

**KPIs to instrument.**
- Queue latency (submit → decision); first-pass approval rate.
- Rejection reason distribution; repeat-violation rate per supplier.
- Rule precision (flags upheld / total flags); reviewer throughput.

---

### A3. RFQ/spam moderation, abuse-report handling, account suspension

**Persona & goal.** `PLATFORM_MODERATOR` and `PLATFORM_ADMIN` handle flagged RFQs (spam, off-topic, price-fishing), user-submitted abuse reports (messages, profiles, products), and — where warranted — suspend accounts or organizations, with due process and a complete audit trail.

**Entry points.** `/admin/moderation` (RFQ tab); `/admin/reports` (abuse reports from X4 and public report links); automated spam-rule flags; support escalations (`PLATFORM_SUPPORT`).

**Preconditions.** Appropriate platform role; suspension authority restricted to `PLATFORM_ADMIN`/`SUPER_ADMIN`; moderators can recommend but not execute suspensions.

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/admin/moderation` | Reviews flagged RFQ | RFQ content + rule/report rationale + publisher context (org verification, RFQ history, prior strikes); RFQ in `PENDING_REVIEW` is invisible to suppliers (B4 step 6). |
| 2 | Same | Decides: Release / Reject / Reject + strike | Release → RFQ `OPEN`, matching notifications fire (deferred from publish). Reject → buyer notified with reasons; RFQ `REJECTED`. Strikes accumulate on the org ledger. |
| 3 | `/admin/reports` | Opens an abuse report (e.g. harassing messages, fake supplier) | Report shows reported content in context (thread excerpt with reporter's selection, X4), reporter identity (internal only), reported party's ledger; duplicate reports auto-grouped. |
| 4 | `/admin/reports/[id]` | Investigates | Read access to relevant threads/content is permission-gated and audit-logged (privacy: access recorded, minimum necessary scope); moderator records findings. |
| 5 | Same | Resolves: dismiss / warn / content removal / recommend suspension | Warn → formal notice to the party. Content removal → content hidden with tombstone (X4). Recommendation → escalates to `PLATFORM_ADMIN` with dossier. |
| 6 | `/admin/organizations/[id]` | (`PLATFORM_ADMIN`) suspends org or user | Confirmation with scope (user vs org), duration (temporary/indefinite), and reason. Effects: sessions invalidated; public pages delisted (V1/V2 edge states); open RFQs/quotations frozen with counterparty notifications; messaging locked read-only (X4). Fully audit-logged; `SUPER_ADMIN` can reverse. |
| 7 | Same | Handles appeal | Suspended party's appeal (via support channel) reviewed by a different admin than the suspender where staffing allows; outcome logged; reinstatement restores state with a re-listing delay for search surfaces. |

**Success criteria.** No spam RFQ reaches suppliers' matched notifications; every enforcement action has reasons + audit trail + notice to the affected party; counterparties of frozen artifacts are informed without exposing investigation details; appeals path exists and is tracked.

**Failure & edge states.**
- Reporter abuse (weaponized reporting) → reporter-side ledger; repeated bad-faith reports rate-limited.
- Suspension of an org mid-award (B6) → award snapshot preserved; counterparty notified with support contact; no silent data loss.
- Automated spam rule flags a legitimate high-volume buyer → release + rule-tuning feedback; publisher informed of delay, not of rule internals.
- Conflicting concurrent actions (moderator releasing while admin suspends) → org-level state takes precedence; conflicts surfaced.
- Legal/urgent takedown → `SUPER_ADMIN` fast path with post-hoc audit review.

**Notifications/emails.**
- Publisher: RFQ released/rejected (with reasons); warning notices; suspension notice with appeal instructions.
- Reporter: report received; report closed (generic outcome).
- Counterparties: artifact-frozen notices (neutral language).
- Admins: escalation and appeal assignments.

**KPIs to instrument.**
- Report volume by type; time-to-resolution; report-upheld rate.
- Spam-RFQ catch rate pre-notification (rule + queue effectiveness).
- Suspension count/duration; appeal rate and overturn rate.
- Repeat-offense rate post-warning.

---

## Cross-cutting journeys

### X1. Password reset

**Persona & goal.** Any registered user who cannot sign in regains access securely.

**Entry points.** `/auth/login` → "Forgot password?"; support-directed link.

**Preconditions.** Account exists (flow must not reveal whether it does).

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/auth/login` | Clicks "Forgot password?", enters email | Uniform response regardless of account existence ("If an account exists, we've sent a link") — no enumeration; rate-limited per email and IP. |
| 2 | Email client | Opens reset email, clicks link | Single-use token, short expiry; link opens reset form; token bound to the request context. |
| 3 | `/auth/reset-password?token=…` | Sets new password | Password policy enforced with live feedback; on success: token consumed, all other sessions and refresh tokens revoked, outstanding email-verification/reset tokens invalidated. |
| 4 | `/auth/login` | Signs in with new password | If MFA enrolled (X2), TOTP challenge still applies — reset does not bypass MFA. Redirect to last org context (X3). |

**Success criteria.** User regains access without support intervention; no account-existence leakage; all pre-reset sessions terminated.

**Failure & edge states.**
- Expired/used token → clear message + restart option.
- Email deliverability failure → generic UI unchanged (no oracle); user may retry after cooldown; support path for persistent failure.
- Reset requested while account suspended → email states suspension and appeal path instead of reset link, per policy.
- MFA device lost + password forgotten → identity-verification support process (recovery codes from X2 are the self-serve path).

**Notifications/emails.**
- Email: reset link; "your password was changed" security notice (with "wasn't you?" support link) after completion.

**KPIs to instrument.**
- Reset requests per active user; request → completed-reset rate; median completion time.
- Post-reset login success rate; support escalations for account recovery.

---

### X2. MFA/TOTP enrolment

**Persona & goal.** A security-conscious user (or one required by org policy) adds TOTP two-factor authentication.

**Entry points.** `/buyer/settings` or `/supplier/settings` → Security; org-policy prompt at login ("your organization requires MFA"); post-registration security nudge.

**Preconditions.** Authenticated with a recent re-authentication (password confirm) before security changes.

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `…/settings/security` | Clicks "Enable two-factor authentication" | Re-authentication challenge (password, recent-session check). |
| 2 | Same | Scans QR code (or copies secret) into authenticator app | TOTP secret generated server-side, displayed once; standard otpauth URI; app-agnostic instructions. |
| 3 | Same | Enters current 6-digit code | Code verified with clock-drift window; on success MFA state `ENABLED`. |
| 4 | Same | Saves recovery codes | One-time set of recovery codes displayed for download/copy; user must confirm storage before finishing; codes hashed at rest. |
| 5 | `/auth/login` (subsequent) | Signs in: password → TOTP prompt | TOTP (or recovery code) required; recovery-code use decrements the set and triggers a security notice; "remember this device" per policy. |
| 6 | `…/settings/security` | Later: regenerate codes / disable MFA | Both require re-authentication + current TOTP; org-mandated MFA cannot be self-disabled while policy applies. |

**Success criteria.** MFA verified end-to-end before enforcement (no lockout from a mistyped setup); recovery path confirmed stored; all changes produce security notifications.

**Failure & edge states.**
- Wrong code repeatedly during setup → setup abandoned safely (MFA not half-enabled); retry with same or regenerated secret.
- Clock drift → tolerance window; guidance to sync device time.
- All recovery codes consumed → forced regeneration prompt at next login.
- Lost device + no recovery codes → support identity-verification process (X1 edge); no email-only bypass.
- Org policy toggled to "MFA required" → grace period with login interstitial until enrolled.

**Notifications/emails.**
- Email: MFA enabled / disabled / recovery code used / recovery codes regenerated (security notices).

**KPIs to instrument.**
- MFA adoption rate (overall, and among ORG_OWNERs/admins); enrolment funnel completion.
- Recovery-code usage rate; MFA-related lockouts and support tickets.
- Org-policy-driven vs voluntary enrolment share.

---

### X3. Multi-organization switching

**Persona & goal.** A consultant or group employee belongs to multiple organizations (e.g. a buying office and a supplier it represents, or two sister companies) and must switch contexts without cross-contamination of data or permissions.

**Entry points.** Global org switcher in the portal header (all `/buyer/*`, `/supplier/*` shells); post-login org picker; deep links into another org's portal.

**Preconditions.** User has ≥2 active org memberships (via B1 creation and/or S6 invitations).

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/auth/login` | Signs in | If multiple orgs: org picker (or auto-select last-used context); session carries an active-org claim. |
| 2 | Any portal page | Opens org switcher | Menu lists memberships with org name, type (buyer/supplier), role, verification badge; current context clearly marked. |
| 3 | Same | Selects a different org | Active-org context switches server-side (claims refreshed); redirect to that org's appropriate portal home (`/buyer/dashboard` vs `/supplier/dashboard` by org capability); all subsequent API calls scoped to the new org. |
| 4 | Portal | Works in new context | Favorites, saved searches, drafts, threads, notifications shown for the active org context only (user-scoped items like personal settings persist across); notification center groups items by org with context labels. |
| 5 | Deep link | Opens a link belonging to org B while active in org A | Resource's org resolved; if user is a member of org B → context-switch interstitial ("Switch to Acme Optical to view this RFQ?"); if not a member → 404-equivalent (no existence leakage). |

**Success criteria.** No data from org A ever renders in org B's context; switching is one interaction plus confirmation; deep links never dead-end for legitimate members; audit logs record the acting org context on every write.

**Failure & edge states.**
- Membership revoked while session active in that org → next request fails closed; user bounced to org picker with notice.
- Org suspended (A3) → context selectable but portal shows suspension notice only.
- User's only org deleted → falls back to "no organization" state (B1 edge).
- Same user is buyer in org A and supplier in org B on the same RFQ → conflict-of-interest rule: system flags per policy (minimum: both orgs' actions logged; optional: block quoting on own org's RFQ).
- Concurrent tabs in different org contexts → active-org bound to session token, not tab; UI detects mismatch (stale context banner + refresh) rather than silently mixing writes.

**Notifications/emails.** In-app notification center labels each item's org; emails state the org context in subject/body ("[Acme Optical] New quotation received").

**KPIs to instrument.**
- Multi-org user share; switches per multi-org user per week.
- Wrong-context error rate (denied requests due to stale context) — should trend to ~0.
- Deep-link interstitial acceptance rate.

---

### X4. Messaging thread lifecycle: attachments, read state, blocking/reporting

**Persona & goal.** Buyer and supplier participants conduct negotiations in database-backed threads (RFQ-linked, product-linked, or general org-to-org), exchange files, track read state, and have recourse against abuse. This journey defines behavior referenced by B3, B5, B6, S4, S5, A3.

**Entry points.** "Contact supplier" (B3); RFQ-linked threads (B4–B6, S4–S5); `/buyer/messages` and `/supplier/messages` inboxes; notification/email deep links.

**Preconditions.** Both orgs active; sender has messaging permission in their org; thread context (product/RFQ) valid at creation. One thread per (buyer org, supplier org, context) tuple — repeat contact reuses the thread.

| Step | Surface/route | User action | System behavior |
|------|---------------|-------------|-----------------|
| 1 | `/buyer/messages` (or supplier) | Opens inbox | Threads with counterparty org (+ badge), context chip (product/RFQ/general), snippet, unread count, last activity; filters by context type and unread; org-scoped (X3). |
| 2 | `…/messages/[threadId]` | Opens a thread | Messages with sender attribution (person + org), timestamps in viewer's timezone, context card pinned (product summary or RFQ/quotation state); opening marks messages read for the viewer (per-user read state; org-level unread derives from members per policy). |
| 3 | Same | Sends a message with attachment | Attachment validated (allowed types incl. PDF/XLSX/images/CAD-adjacent formats per policy, size caps, malware scan quarantines failures); message persisted, delivered; recipient unread counters update; anti-spam limits for first-contact messages (rate + content rules feeding A3). |
| 4 | Counterparty portal | Recipient reads and replies | Read state visible to sender at thread level ("Read" indicator, policy-flagged for org privacy defaults); reply triggers in-app notification and email per recipient preferences (immediate vs digest; email contains excerpt + deep link, honoring unsubscribe granularity). |
| 5 | Same | Ongoing lifecycle | System events posted inline (quotation revised, RFQ closed, invitation revoked) as non-editable system messages; threads never hard-deleted by users — archive hides from inbox, restorable. |
| 6a | Same | Participant blocks the counterparty org | Block confirmation explains effects: counterparty can no longer start threads or message the blocker's org; existing thread locked read-only for both with a neutral notice to the blocked party; RFQ invitations between the orgs prevented (B5 edge); block reversible by the blocking org's admins. |
| 6b | Same | Participant reports a message/thread | Report dialog: category (spam, harassment, fraud, counterfeit, off-platform payment solicitation), selected messages attached as evidence, optional note → creates `/admin/reports` case (A3); reporter sees "report received" state; thread remains usable unless the reporter also blocks. |
| 7 | Same | Terminal states | Org suspension (A3) → threads read-only with system notice. Context deletion (product unpublished) → thread persists with tombstoned context card. Data retention per legal policy; participants can export their threads (compliance feature, policy-flagged). |

**Success criteria.** Message delivery is durable and ordered; read state accurate per user; attachments never bypass scanning; blocking takes effect immediately across messaging and invitations; every report reaches the admin queue with evidence attached.

**Failure & edge states.**
- Send fails (network) → optimistic UI with retry and unsent-state marker; no silent loss or duplicates (client idempotency keys).
- Attachment quarantined by scanner → sender notified; message body still delivered without the file.
- Recipient org has zero members with messaging notifications enabled → platform nudges org admins about unattended inbox after N days.
- Blocked party attempts contact → composer disabled with neutral "not available" state (no confirmation of blocking).
- Both orgs block each other, then one unblocks → thread remains locked until both blocks lifted.
- Thread deep link after membership loss → 404-equivalent (X3 rule).
- Messages containing off-platform contact pressure (policy) → soft warning to sender; rule hits logged for A3 patterns.

**Notifications/emails.**
- Recipient: new-message in-app + email (immediate or digest, per preference and quiet hours); first-contact messages flagged distinctly.
- Sender: delivery-failure notices only (no read-receipt emails).
- Reporter: report received / closed (A3).
- Blocked-org admins: no notification of being blocked (neutral degradation).

**KPIs to instrument.**
- First-response rate and median first-response time per supplier (candidate public profile stat — only if displayed under clear methodology).
- Messages per thread; thread reactivation rate (dormant > 14 days then resumed).
- Attachment usage and quarantine rate.
- Block rate, report rate, report-upheld rate (feeds A3 quality metrics).
- Notification email CTR into threads (re-engagement).

---

## Cross-journey notes

1. **Auth gates preserve intent.** Every anonymous-to-authenticated transition (V1 step 5, V2 step 6, V3 step 7, B3 step 4) carries a `returnTo` and a server-side pending-action record so the user's task resumes after login/registration. This is the systemic correction to the incumbent's registration-wall dead ends.
2. **State machines over flags.** RFQs (`DRAFT → PENDING_REVIEW? → OPEN → AWARDED | CLOSED_NO_AWARD | EXPIRED | REJECTED`), quotations (`SUBMITTED → REVISED* → AWARDED | NOT_AWARDED | EXPIRED | WITHDRAWN`), verification (`UNVERIFIED → PENDING_REVIEW → VERIFIED | NEEDS_MORE_INFO | REJECTED`, plus `REVERIFICATION_REQUIRED`), and invitations (`INVITED → VIEWED → QUOTED | DECLINED | REVOKED`) are the canonical vocabularies used across buyer, supplier, and admin journeys; UI copy must map 1:1 to these states.
3. **Trust signals are computed, never asserted.** Verification badges, response-time stats, and any profile metrics render only from reviewed or measured data; absence of data renders as absence, not placeholder claims (per the audit's no-fabricated-metrics constraint).
4. **Notifications are preference-governed.** Every email type listed above must be individually controllable (immediate/digest/off) except security notices (X1, X2) and legally required notices (suspension), which are mandatory.
5. **Policy-flagged decisions.** Items marked "per policy" in journeys (pricing visibility to anonymous users, unverified-supplier quoting, read-receipt defaults, unverified-profile listing) are product decisions pending stakeholder sign-off; each has a stated default so implementation is unblocked.
6. **Instrumentation baseline.** All KPI events flow through a consent-gated analytics layer (correcting the incumbent's unconditional trackers); funnel step names in this document are the canonical event taxonomy seeds.
