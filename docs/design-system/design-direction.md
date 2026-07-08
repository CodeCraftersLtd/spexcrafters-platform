# SpexCrafters — Design Direction

**Project:** SpexCrafters — premium B2B optical-industry marketplace
**Phase:** 1 — Visual direction (three concepts + recommendation)
**Date:** 2026-07-08
**Status:** Proposed — awaiting stakeholder approval
**Inputs:** `CURRENT_SITE_AUDIT.md` §6.2 (no design system, unharmonized palette, single-weight Inter, vw typography, no brand assets), §6.3 (blocked zoom, no focus strategy, no reduced-motion handling, silent loading states)

---

## 0. Design brief distilled

The incumbent has **no visual identity**: default Element Plus/Vant components, one weight of Inter, five near-identical oranges, a loading GIF as the sole brand asset, and hard WCAG failures (audit §6.2–6.3). SpexCrafters must therefore establish, from zero:

1. **A brand that signals instrument-grade competence.** Buyers here are opticians, lab managers, and procurement teams who read dioptre tables and ISO cert numbers for a living. The design must speak their language: precision, verification, specification.
2. **Award-level art direction that never taxes usability.** Craft lives in typography, spacing, line-work, and restraint — not in decoration that costs LCP milliseconds or violates WCAG 2.2 AA.
3. **Two register system:** an editorial public web (SEO-critical, SSR) and a data-dense authenticated app (RFQ, quotations, orders, dashboards) sharing one token system.

Non-negotiable constraints for all three concepts: WCAG 2.2 AA, Lighthouse ≥90 perf / ≥95 a11y-BP-SEO, CWV green, self-hostable fonts, `prefers-reduced-motion` honored everywhere, no third-party font/CSS CDNs.

---

## 1. Concept A — MERIDIAN: Instrument-Grade Precision

### 1.1 Creative idea

Every optical product is born on an instrument: a lensmeter, a focimeter, a phoropter. These machines share a visual grammar — hairline reticles, engraved scales, dioptre markings, a single indicator needle against a calm field. MERIDIAN adopts that grammar as brand language: **the marketplace as a calibrated instrument for the optical trade.** Where a generic B2B site says "trust us," MERIDIAN *demonstrates* precision in every rule, label, and number. The brand promise is legibility under load — the same quality a lab manager wants from a lensmeter at 6 p.m. on a Friday.

### 1.2 Visual language

- **Hairline rules** (1px, ink-200/ink-800) structure everything: section dividers, table grids, card boundaries. Borders do the work shadows do elsewhere; shadows are reserved for true overlays.
- **Measurement-grid backgrounds:** a faint 8px engineering grid (SVG pattern, ≤1 KB, <4% opacity) behind hero and section headers only — read as graph paper, never as texture noise.
- **Technical annotations:** small mono-spaced labels in the margins of key surfaces — `FIG. 01`, `SPH −8.00 → +6.00`, `MOQ 6 / LEAD 12–15 D`, `VERIFIED 2026-05` — echoing lensmeter engraving. Decorative annotations are `aria-hidden`; data annotations are real, sourced values.
- **Crosshair/reticle motif** as the icon-system seed: the logo mark, focus states, map pins, and empty states all derive from a circle-plus-tick reticle.
- **Dense-but-calm data:** generous line-height and whitespace *around* dense spec tables, never inside them. The tension between editorial calm and tabular density is the aesthetic.
- **Photography:** products on neutral technical gray sweeps with a faint scale ruler in frame; factory photography in natural light, honest, uncolored — documentation, not advertising.

### 1.3 Typography direction

| Role | Typeface | License | Notes |
|---|---|---|---|
| Display / editorial | **Archivo** (variable, incl. Expanded width axis) | OFL 1.1 — free, self-hostable | Grotesk with engineering DNA; Expanded width for hero/section headers gives instant differentiation from Inter-everywhere SaaS |
| UI / text | **Instrument Sans** (variable) | OFL 1.1 | Purpose-named coincidence aside: excellent at 13–16px, tight UI metrics, true variable weight 400–700 |
| Mono / data | **IBM Plex Mono** | OFL 1.1 | Tabular figures, wide language coverage (incl. planned zh-Hans pairing via Plex Sans SC for CJK fallback) |
| Commercial upgrade (optional, later) | Söhne + Söhne Breit + Söhne Mono (Klim) | Commercial, self-host webfont license | A single-family system; upgrade is a font-file swap because roles/tokens stay identical |

Two variable files (Archivo, Instrument Sans) + two static Plex Mono weights ≈ **~220 KB woff2 total**, subset to Latin-Ext initially. `font-display: swap` with metric-compatible fallbacks (`Arial` adjusted via `size-adjust`) to kill CLS.

### 1.4 Color philosophy

Near-monochrome: warm-white "paper" surfaces, cool near-black "ink" text, and **one calibrated accent** — a cobalt drawn from anti-reflective coating flare — used *only* for interaction (links, primary actions, focus, selection). Status colors exist but are muted and appear only on status semantics. If a screen has no interaction and no status, it is entirely ink-on-paper.

Indicative ramps (full verified ramps in `design-system.md`):

| Ramp | Stops |
|---|---|
| Paper | `#FDFDFB` → `#F7F7F4` → `#F1F1EC` |
| Ink | `#F0F1F3` `#CDD2D9` `#A6AEB8` `#7A8492` `#5C6672` `#444D58` `#2E353E` `#14171C` `#0C0E12` |
| Accent (calibrated cobalt) | `#EDF1FD` `#B3C4F4` `#86A0EC` `#5A7BE0` `#3A5CCC` `#2947B8` `#1F3691` `#101D4E` |
| Status | success `#1B6E45` · warning `#8A5A00` · danger `#BA2E2E` · info `#0B5E8A` (all ≥4.5:1 on paper — see token spec) |

### 1.5 Motion principles

Motion behaves like an instrument: **short, damped, precise.** Nothing bounces, nothing floats.

- **CSS only** for ~95% of motion: opacity/transform micro-transitions (80–160 ms), focus reticle draw (a 120 ms corner-tick animation on `:focus-visible`), accordion/detail expansion, skeleton shimmer.
- **Motion for React** for orchestrated UI state: dialog/drawer enter-exit, toast stacking, list reorder (RFQ comparison), tab indicator slide. Spring configs tuned stiff (no overshoot).
- **GSAP: not used.** MERIDIAN's hero is typographic + line-drawn; `IntersectionObserver` + CSS handles scroll reveals.
- **Signature micro-moment:** "calibration tick" — numbers (prices, counts, dioptre ranges) settle with a 3-frame tabular-figure tick, like a digital lensmeter locking a reading. Disabled under reduced motion.
- `prefers-reduced-motion: reduce` → all transitions collapse to opacity ≤100 ms; ticks/reveals render final state instantly. Enforced by a single token-level media query, not per-component opt-in.

### 1.6 Homepage hero concept

Full-bleed paper surface over the faint measurement grid. A single oversized Archivo Expanded statement — *"The optical industry, calibrated."* — with one line of the headline intersected by a drawn hairline rule carrying live mono annotations (`3,412 VERIFIED SKUs · 28 COUNTRIES · SPH −12.00 → +8.00` — real values only, per audit §10.4). Right-aligned: a single product photographed instrument-style with a scale ruler. Below the fold: three hairline-ruled entry cards (Source products / Find suppliers / Post an RFQ). No carousel, no video, LCP element is the headline text.

### 1.7 Marketplace UI concept

- **Listing page = catalog spec-sheet.** Left facet panel with hairline-separated groups (lens index, material, coating, SPH/CYL range, MOQ, lead time — the parameters audit §6.1.7 says are currently unsearchable). Facet counts in mono.
- **ProductCard:** photo on paper, human name over SKU code in mono, then a three-cell hairline strip: `MOQ 6` · `$2.10–3.40 tiered` · `12–15 d lead`. Wholesale mechanics surfaced at card level — directly fixing audit §6.1.3.
- **SpecTable** is the detail-page hero: measurements, Rx ranges, coatings in a ruled table with mono values — the product's "instrument readout."
- Density toggle (comfortable/compact) for professional buyers; state persisted per user.

### 1.8 Supplier page concept

A **calibration certificate** metaphor: header band with supplier name in Archivo Expanded, a reticle-derived VerifiedBadge with verification date in mono, then ruled sections — Capabilities (machinery, capacity, tolerances), Certifications (CE/FDA/ISO chips linking to documents), Export markets (a restrained ink-line map), Product lines, and an RFQ call-to-action. Reads like a document you could print and file — which procurement teams literally do.

### 1.9 Assessment

| | |
|---|---|
| **Advantages** | Deeply ownable in this vertical (metrology grammar = optical industry's own language); scales perfectly from editorial pages to dense dashboards; cheapest concept to render (text, hairlines, one SVG pattern); typography does the branding, so it survives feature growth for a decade; hardest of the three to mistake for a Tailwind template |
| **Risks** | Restraint executed lazily reads as unfinished — demands excellent typesetting discipline; near-monochrome needs careful photography direction to avoid sterility; annotation motif can tip into affectation if unrationed (ration: max one annotation cluster per viewport) |
| **Performance** | Best of the three. No hero imagery required for LCP; ~220 KB fonts; grid pattern ≤1 KB; zero animation library on public pages. Lighthouse ≥95 perf realistic |
| **Accessibility** | Strongest baseline: high-contrast ink/paper by default; hairline borders that convey meaning are specified at ≥3:1 (ink-400); annotations are `aria-hidden` when decorative; motion trivially reducible; monochrome-first design is inherently color-blind-safe (color never the sole carrier) |

---

## 2. Concept B — SPECTRA: Controlled Refraction

### 2.1 Creative idea

The entire industry exists because **light bends through glass**. SPECTRA makes that one physical fact the brand: a single, signature spectral-dispersion gradient — white light entering, a disciplined spectrum leaving — reserved exclusively for hero and brand moments. Everywhere else the interface is austere white-and-ink, so that when the spectrum appears it lands with the force of scarcity. The brand promise: *we handle light with precision.*

### 2.2 Visual language

- **The signature dispersion:** one canonical gradient (violet→cobalt→cyan→green→amber→red, tuned to a common lightness band) applied as a thin refracted band or edge-glow — never as a background wash, never on text, never on more than one element per page.
- **Glass-and-light photography:** macro lens edges, caustics on paper, prism refractions — commissioned, high-contrast, on white.
- **Austere ink/white surfaces** with generous editorial margins; the serif provides warmth the palette withholds.
- Occasional **chromatic-fringe accent** on the hero image edges only (2px RGB offset), quoting lens aberration — the flaw the industry engineers away, controlled.

### 2.3 Typography direction

| Role | Typeface | License | Notes |
|---|---|---|---|
| Display / editorial serif | **Fraunces** (variable: opsz, SOFT, WONK axes) | OFL 1.1 | High-contrast display serif; opsz axis gives real editorial range |
| UI / text grotesk | **Space Grotesk** (variable) | OFL 1.1 | Crisp, slightly technical counterweight to the serif |
| Mono / data | **Fragment Mono** | OFL 1.1 | Light-duty; data is not this concept's center of gravity |
| Commercial upgrades | GT Sectra or Canela (display); Suisse Int'l (UI) | Commercial self-host | Sharper editorial voice when budget allows |

### 2.4 Color philosophy

White `#FFFFFF` / ink `#111318` as the entire working palette; interactive elements use ink itself (underlines, fills) plus a deep spectral-blue `#2440B3` for links/focus (≥7:1 on white). The dispersion gradient — indicative stops `#6E56CF → #3E63DD → #00A2C7 → #30A46C → #FFC53D → #E5484D` — is a **decorative-only token**: it may never carry text or meaning, so it has no contrast obligation. Status colors as in MERIDIAN.

### 2.5 Motion principles

- **CSS:** light-sweep reveals (a 200 ms specular highlight passing across cards on first viewport entry), underline draws, standard micro-transitions.
- **Motion for React:** overlay choreography, shared-element product-image transitions on the public site.
- **GSAP (+ ScrollTrigger): homepage hero only** — a 3-act scroll-driven sequence: white beam → enters a drawn lens profile → disperses into the signature spectrum → resolves into the marketplace value proposition. Lazy-loaded after LCP, ~35 KB gz, route-scoped, fully skipped under reduced motion (static composed frame with the same copy).
- Reduced motion: sweeps and fringes removed entirely (they are aesthetic, not informational); hero renders its final frame.

### 2.6 Homepage hero concept

The GSAP light-dispersion narrative above, art-directed like a physics plate: Fraunces headline (*"Where light meets trade."*), the beam/spectrum drawn as SVG strokes (not video, not WebGL). Static-image fallback is the LCP element and is served to reduced-motion, data-saver, and low-end devices.

### 2.7 Marketplace UI concept

Editorial catalog: larger imagery, Fraunces category headers, generous cards on white with ink hairlines. The spectrum appears once — a 2px refracted keyline under the search bar. Facets and spec tables in Space Grotesk/Fragment Mono, visually quieter than MERIDIAN's but structurally similar.

### 2.8 Supplier page concept

Magazine-profile treatment: full-width duotone-free factory photograph, Fraunces pull-quote from the supplier, then structured capability/cert sections. VerifiedBadge rendered as a spectral micro-edge on an ink seal — the one place the gradient appears on marketplace pages.

### 2.9 Assessment

| | |
|---|---|
| **Advantages** | The most emotionally striking of the three; the dispersion signature is a genuinely ownable brand device rooted in the product physics; serif+grotesk pairing photographs beautifully for award juries and LinkedIn shares (the actual B2B channel, audit §6.5.5) |
| **Risks** | Gradient scarcity requires governance — one careless PM request ("make it pop") and it becomes gradient soup; editorial serif register can undercut data-dense dashboard credibility; commissioned photography is a real budget/timeline dependency; GSAP hero is a maintenance liability owned by a marketing page |
| **Performance** | Good with discipline: GSAP route-scoped to `/` (+~35 KB), hero SVG cheap, but photography direction pushes image weight; dashboard pages unaffected. Lighthouse ≥90 achievable, less headroom than MERIDIAN |
| **Accessibility** | Sound if rules hold: gradient is decorative-only (no contrast dependency); chromatic fringe capped at 2px and removed under reduced motion (vestibular + photosensitivity safety); serif display faces need minimum-size floors (≥20px) for readability; scroll-jacking is prohibited — hero animates *with* native scroll, never hijacks it |

---

## 3. Concept C — LATTICE: The Trade Network

### 3.1 Creative idea

SpexCrafters's actual product is not glass — it is **connection**: Shenzhen coating lines to Lyon labs, Busan machinery to Nairobi retail chains. LATTICE visualizes the network itself: great-circle routes, node constellations, wavefront contours (borrowed from wavefront aberrometry — an optics-native pattern language). Public site light and editorial; authenticated dashboards dark, like a logistics control room. The brand promise: *you are plugged into the global optical trade.*

### 3.2 Visual language

- **Node/route line-work:** 1px plotted arcs and nodes as section ornaments, map visualizations, and the logo system (three nodes, two routes).
- **Wavefront contour patterns** (concentric distorted rings from aberrometry maps) as rare section-header textures.
- **Dual-surface world:** paper-white public site; ink-dark (`#0B0D12`) "control room" for dashboards, messaging, RFQ management — with glowing route-work used sparingly.
- **Mono-spaced data accents** everywhere numbers meet the trade: HS codes, lead times, port names, quote deltas.

### 3.3 Typography direction

| Role | Typeface | License | Notes |
|---|---|---|---|
| Display / UI grotesk | **Familjen Grotesk** (variable) | OFL 1.1 | Warm, slightly quirky grotesk; differentiates from geometric-SaaS default |
| Mono / data (co-lead role) | **JetBrains Mono** (variable) | OFL 1.1 | The concept's signature voice — used for display-scale numbers, not just code |
| Secondary text | **Hanken Grotesk** (variable) | OFL 1.1 | Long-form fallback where Familjen's quirks tire |
| Commercial upgrades | Founders Grotesk (display); Berkeley Mono (data) | Commercial self-host | Berkeley Mono in particular elevates the control-room register |

### 3.4 Color philosophy

Ink-dark control-room ramp (`#0B0D12` → `#161A22` → `#232936`) with **signal green** `#3ECF8E`-family accents for live/positive data and a route-cyan `#4CC3E8` for network visualization; light public site reuses the same hues darkened for contrast (`#1D7A4A`, `#0B5E8A`-adjacent). Amber/red reserved for genuine alerts. Risk acknowledged: green-on-dark reads "developer tool" — mitigated by warm paper tones on public pages.

### 3.5 Motion principles

- **CSS:** node pulse (opacity only), hover states, contour parallax ≤8px.
- **Motion for React:** dashboard widget mount choreography, live-updating counters, list diffing in quote comparisons.
- **GSAP + SVG `stroke-dashoffset`** (or plain CSS where possible): plotted-line route drawing on the homepage map and supplier "export markets" maps.
- **Data-stream micro-animation** (activity feeds ticking in) is capped at one live region per view, `aria-live="polite"`, pausable, and static under reduced motion.
- Reduced motion: routes render fully drawn; pulses stop; counters jump to final values.

### 3.6 Homepage hero concept

An ink-line world map (single-color SVG, no basemap tiles) with routes drawing between real trade nodes, headline in Familjen Grotesk (*"The network behind every lens."*), live-labeled route annotations in JetBrains Mono. Map is decorative-summary (`role="img"` with alt narrative); the same data appears as text below.

### 3.7 Marketplace UI concept

Light editorial catalog with network garnish: supplier origin shown as node-chip (`CN · Shenzhen`), lead-time as a mini route glyph, facets as in MERIDIAN. Cards carry a thin route-line footer connecting origin → destination estimate.

### 3.8 Supplier page concept

Split identity: public profile light with an export-market route map as the hero; the supplier's own dashboard dark ("their control room") — capacity, open RFQs, message threads with mono timestamps. Strongest concept for the logged-in supplier experience.

### 3.9 Assessment

| | |
|---|---|
| **Advantages** | Best story for the *marketplace/network* positioning (the strategic pivot from single-vendor shop, audit §1); dark control-room dashboards genuinely aid long-session data work; map/route system doubles as real UI (export markets, logistics tracking) |
| **Risks** | Cartography + dark-dashboard aesthetics are the current fintech/logistics cliché — differentiation decays fastest; dual light/dark register doubles art-direction and QA surface from day one; map ornamentation tempts data-fabrication ("fake glowing routes") which audit §10.4 explicitly forbids; network metaphor says little about *optics* specifically |
| **Performance** | Moderate: SVG maps are cheap but route animation + live feeds add main-thread work on the heaviest pages (dashboards); two full theme surfaces to keep within budget |
| **Accessibility** | Hardest of the three: dark-theme contrast must be re-verified for every token; glowing/pulsing elements need photosensitivity limits (<3 flashes/s trivially, but also sheer restraint); live data streams require careful `aria-live` throttling; maps need full text equivalents |

---

## 4. Recommendation — **MERIDIAN with a spectral signature**

**Recommended direction:** **MERIDIAN (base system) + SPECTRA's single spectral-dispersion signature reserved for brand moments.** LATTICE is not adopted, but its dark control-room register survives as MERIDIAN's dashboard dark theme (already required — see token spec §4), and its mono-data-accent instinct is native to MERIDIAN anyway.

### 4.1 What the merge means, precisely

- MERIDIAN supplies **everything structural**: tokens, type system, color system, components, motion policy, dashboard density.
- From SPECTRA we take exactly **one asset**: the canonical spectral-dispersion gradient, tokenized as `--sc-gradient-spectral` and permitted in an enumerated whitelist of brand moments:
  1. the logo mark's refracted edge,
  2. the homepage hero's single keyline,
  3. the VerifiedBadge micro-edge (the platform's highest-trust symbol carries the brand's one moment of light),
  4. empty/celebration states (first RFQ sent, verification granted),
  5. marketing/social OG imagery.
- **Nowhere else.** The gradient never carries text, never exceeds a 4px stroke or edge treatment in UI, never appears twice in one viewport, and its use requires design-system-owner sign-off (enforced by lint: only whitelisted components may reference the gradient token).
- GSAP is **not** inherited from SPECTRA. If a scroll-story hero is later commissioned, it enters as a route-scoped exception with its own performance budget line — the base system does not depend on it.

### 4.2 Justification against the decision criteria

| Criterion | Assessment |
|---|---|
| **Differentiation** | Metrology grammar (reticles, hairlines, engraved annotations, calibrated cobalt) is drawn from the customer's own instruments — no horizontal SaaS or commodity-marketplace competitor can claim it credibly. The rationed spectral signature adds the memorable "one moment of light" that pure MERIDIAN risks lacking in brand-channel contexts (logo, OG cards, LinkedIn — audit §6.5.5). SPECTRA alone is more spectacular but less defensible day-to-day; LATTICE is the fastest-decaying cliché. |
| **B2B trust** | Procurement teams and lab managers equate visual precision with operational precision. Ruled spec tables, mono-set tolerances, dated verification marks, and a near-monochrome field say "audited supplier ledger," not "growth-hacked storefront." This directly rebuilds the trust deficit in audit §6.6 (no certs, no company identity, template filler copy). |
| **Data density (dashboards)** | MERIDIAN is the only concept whose *core* aesthetic is dense-but-calm tabular data — RFQ comparison tables, quotation matrices, SPH/CYL grids, and order dashboards are its natural habitat, not an afterthought to a hero concept. The hairline/8px-grid system scales to a compact density mode without inventing a second language. |
| **Performance budget** | Text-first hero (LCP = headline), ~220 KB self-hosted variable fonts, ≤1 KB SVG grid pattern, zero animation library on public routes, no mandatory hero photography or WebGL. The spectral signature is a CSS gradient — 0 bytes of asset weight. This is the only concept with comfortable headroom above Lighthouse 90, not a fight to reach it. |
| **Accessibility** | Highest-contrast baseline (ink 17.6:1 on paper); color never the sole information carrier by construction; the gradient is decorative-only so it carries no contrast obligation; motion policy is centrally reducible; dense tables get real `<table>` semantics with `<th scope>` — the concept's aesthetic *is* the accessible pattern, rather than fighting it (contrast with LATTICE's dark/glow re-verification burden). Directly remediates every audit §6.3 failure class. |
| **10-year maintainability** | The system reduces to durable primitives: two OFL variable fonts (swap-upgradeable to Söhne without re-tokenizing), one accent ramp, hairline borders, an 8px grid, and a whitelisted gradient. No dependency on a photography pipeline, an animation library, or a fashionable surface treatment. Trends this survives: it is closer to Swiss typographic modernism + instrument design than to any 2026 web fashion. |

### 4.3 What we will NOT do — guardrails

These are binding. Violations are design-review blockers, and where possible are enforced by lint rules against the token whitelist.

1. **No glassmorphism-everywhere.** No frosted panels, no `backdrop-filter` as a default surface. (Single documented exception candidate: the sticky header may use a subtle blur *if* it passes contrast and scroll-perf budgets — decided once, in the header spec, not per feature.)
2. **No gradient soup.** Exactly one gradient exists in the system (`--sc-gradient-spectral`), usable only in the §4.1 whitelist. No gradient buttons, no gradient text, no gradient card borders, no per-team "brand-ish" gradients.
3. **No floating 3D objects.** No WebGL blobs, no rotating lens models, no parallax product renders. Product truth is photography and spec tables.
4. **No animation for its own sake.** Every animation must answer "what state change does this explain?" Decorative motion is limited to the two signature micro-moments (calibration tick, focus reticle) and is removed under reduced motion. No scroll-jacking, ever.
5. **Never a generic Tailwind SaaS template.** No Tailwind as foundation (tokens + CSS Modules per the system spec); no default shadcn look — Radix primitives may be used headless, skinned entirely by our tokens; no purple-gradient hero, no emoji-bullet feature grids, no rounded-2xl-shadow-xl card fields.
6. **Never an Alibaba clone.** No orange promo chips, no dense badge salad on product cards, no fake urgency ("only 3 left!"), no unverifiable claims or fabricated metrics (audit §10.4) — every number shown is sourced or the module doesn't ship.
7. **No dark-pattern trust theater.** VerifiedBadge appears only on entities that passed the real verification flow; the badge spec includes its revocation state.
8. **No component-library default theming** (the incumbent's core failure, audit §6.2.1). Every rendered control passes through our token layer; a raw Element Plus/MUI/AntD-looking control on screen is a bug.

### 4.4 Consequences & next steps

1. Ratify this direction → freeze the name **"MERIDIAN with a spectral signature."**
2. Token architecture + component inventory: see `docs/design-system/design-system.md` (companion document).
3. Commission: logo/reticle mark exploration; product-photography guideline pilot (5 SKUs); Archivo/Instrument Sans subsetting pipeline.
4. Build order: `/packages/design-tokens` → primitives (Button, Input, focus system) → SpecTable + ProductCard (the two brand-critical composites) → homepage hero.

---

*End of design direction. Companion document: `design-system.md` (token architecture, verified contrast pairs, component inventory, DoD).*
