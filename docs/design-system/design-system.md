# SpexCrafters Design System — "MERIDIAN with a spectral signature"

**Version:** 0.1.0 (initial token architecture + component inventory)
**Date:** 2026-07-08
**Direction:** as ratified in `design-direction.md` (MERIDIAN base + whitelisted spectral signature)
**Stack constraints:** CSS Custom Properties as the styling foundation. **No Tailwind.** CSS Modules per component. Headless primitives (Radix or equivalent) permitted, skinned exclusively by tokens.

---

## 1. Token architecture

### 1.1 Three tiers

| Tier | Prefix pattern | Answers | Example | May reference |
|---|---|---|---|---|
| **1. Primitive** | `--sc-<category>-<name>-<step>` | "What values exist?" | `--sc-color-ink-900: #14171C` | Raw values only |
| **2. Semantic** | `--sc-<category>-<role>[-<variant>][-<state>]` | "What is this value *for*?" | `--sc-color-text-primary: var(--sc-color-ink-900)` | Primitives only |
| **3. Component** | `--sc-<component>-<property>[-<state>]` | "What does this component consume?" | `--sc-button-primary-bg: var(--sc-color-action-primary)` | Semantic (preferred) or primitives (exception, documented) |

Rules:

- Application code (CSS Modules) consumes **semantic and component tokens only**. Referencing a primitive from app CSS is a lint error (Stylelint custom rule against the `--sc-color-ink-*` / `--sc-color-accent-*` patterns outside the tokens package).
- Theming (dark mode, white-label) re-maps **tier 2 only**. Tier 1 is immutable per brand; tier 3 rarely overrides.
- The spectral gradient is a **restricted token**: only components on the whitelist (Logo, HomepageHero keyline, VerifiedBadge, CelebrationState, OG-image templates) may reference `--sc-gradient-spectral`; enforced by the same Stylelint rule with an allowlist of module paths.

### 1.2 Naming convention

```
--sc-{category}-{...}
  categories: color | font | text | space | radius | shadow | border |
              z | motion | size | grid | gradient
```

- `sc` = SpexCrafters namespace (collision-proof against 3rd-party CSS).
- Kebab-case; numeric steps for ramps (`ink-900`), t-shirt or numeric for scales (`space-4`, `text-lg`).
- State suffixes are a closed set: `-hover | -active | -focus | -disabled | -error`.
- No raw hex/px in component CSS Modules except `0`, `1px` hairlines via `--sc-border-hairline`, and `currentColor`.

### 1.3 Distribution — `/packages/design-tokens`

```
packages/design-tokens/
├── src/
│   ├── color.tokens.json          # single source of truth (DTCG format)
│   ├── typography.tokens.json
│   ├── space.tokens.json
│   ├── motion.tokens.json
│   ├── themes/
│   │   ├── light.json             # semantic tier mapping (default)
│   │   └── dark.json              # semantic tier mapping (dashboards)
├── build/                          # generated — never hand-edited
│   ├── css/tokens.css             # :root primitives + [data-theme] semantic blocks
│   ├── css/tokens.dark.css
│   ├── ts/tokens.ts               # typed constants (charting, canvas, email, OG images)
│   └── ts/tokens.d.ts
├── style-dictionary.config.mjs     # Style Dictionary (or Terrazzo) build
└── package.json                    # exports: "./css", "./ts"
```

- **JSON (DTCG `$type`/`$value`) is the source of truth.** CSS and TS are build artifacts; CI fails if `build/` is stale (`pnpm tokens:build --check`).
- TS constants exist for non-CSS consumers: chart libraries, `<canvas>`, OG-image generation, email templates.
- Versioned like a library (semver): renaming/removing a token is a **major**; adding is minor. A codemod ships with every major.

### 1.4 Dark mode & theming strategy

- **Mechanism:** `data-theme` attribute on `<html>` — `data-theme="light"` (default, public web) / `data-theme="dark"` (authenticated dashboards). Semantic tokens re-map under `[data-theme="dark"]`; component CSS never branches on theme.
- **Policy:** public/SEO pages ship light-only in v1 (one contrast-verification surface). The authenticated app defaults to the OS preference (`prefers-color-scheme`) with an explicit user override persisted server-side; the attribute is set inline in the SSR document head to prevent theme flash.
- `color-scheme: light`/`dark` set alongside the attribute so native form controls, scrollbars, and popups match.
- **White-label / future theming:** a tenant theme = a replacement tier-1 accent ramp + logo assets + optional semantic overrides, delivered as one additional CSS file (`theme.acme.css`) loaded after `tokens.css`. Because app CSS only touches tier 2/3, white-labeling never requires component changes. Contrast verification of a tenant ramp is a release gate (automated check in the tokens build).

---

## 2. Color tokens (tier 1 primitives + verified pairs)

### 2.1 Neutrals — ink & paper

| Token | Value | Typical use |
|---|---|---|
| `--sc-color-paper-0` | `#FDFDFB` | Page background (light) |
| `--sc-color-paper-1` | `#F7F7F4` | Raised/alternate surface |
| `--sc-color-paper-2` | `#F1F1EC` | Inset surface, table stripe |
| `--sc-color-ink-50` | `#F0F1F3` | Dark-theme text-strong |
| `--sc-color-ink-100` | `#E4E7EB` | Hairlines on paper (decorative) |
| `--sc-color-ink-200` | `#CDD2D9` | Decorative borders, dividers |
| `--sc-color-ink-300` | `#A6AEB8` | Disabled text (light), secondary text (dark) |
| `--sc-color-ink-400` | `#7A8492` | **Meaningful** borders/icons (≥3:1), placeholder |
| `--sc-color-ink-500` | `#5C6672` | Muted text (light) |
| `--sc-color-ink-600` | `#444D58` | Secondary text (light) |
| `--sc-color-ink-700` | `#2E353E` | Dark-theme raised surface border |
| `--sc-color-ink-800` | `#1F242B` | Dark-theme raised surface |
| `--sc-color-ink-900` | `#14171C` | Primary text (light); dark-theme surface |
| `--sc-color-ink-950` | `#0C0E12` | Dark-theme page background |

### 2.2 Accent — calibrated cobalt

| Token | Value | Typical use |
|---|---|---|
| `--sc-color-accent-50` | `#EDF1FD` | Selected-row wash, subtle info bg |
| `--sc-color-accent-100` | `#D8E1FA` | Hover wash |
| `--sc-color-accent-200` | `#B3C4F4` | Dark-theme link hover |
| `--sc-color-accent-300` | `#86A0EC` | **Dark-theme link/interactive text** |
| `--sc-color-accent-400` | `#5A7BE0` | Dark-theme large interactive elements |
| `--sc-color-accent-500` | `#3A5CCC` | Charts, selection outlines |
| `--sc-color-accent-600` | `#2947B8` | **Primary action / links / focus ring (light)** |
| `--sc-color-accent-700` | `#1F3691` | Primary action hover |
| `--sc-color-accent-800` | `#17296E` | Primary action active |
| `--sc-color-accent-900` | `#101D4E` | Accent-on-accent text |

### 2.3 Status ramps (excerpt — full 50–900 ramps in tokens.json)

| Role | Text-on-light | Bg-subtle | Text-on-subtle | Dark-theme text |
|---|---|---|---|---|
| Success | `#1B6E45` (700) | `#D9F0E2` (100) | `#14522F` (800) | `#6FCF97` (300) |
| Warning | `#8A5A00` (800) | `#FBEFD3` (100) | `#6B4600` (900) | `#F0BE4C` (300) |
| Danger | `#BA2E2E` (600) | `#FADEDE` (100) | `#7D1D1D` (800) | `#F08A8A` (300) |
| Info | `#0B5E8A` (700) | `#D9EDF8` (100) | `#084566` (800) | `#67B7E1` (300) |

### 2.4 Spectral signature (restricted token)

```css
--sc-gradient-spectral: linear-gradient(90deg,
  #6E56CF 0%, #3E63DD 22%, #00A2C7 42%,
  #30A46C 62%, #FFC53D 82%, #E5484D 100%);
```

Decorative-only (never carries text or meaning → no contrast obligation). Whitelist per `design-direction.md` §4.1; max 4px stroke/edge in UI; max one instance per viewport.

### 2.5 WCAG-verified contrast pairs (computed, WCAG 2.x relative-luminance formula)

| Pair (fg on bg) | Ratio | Passes |
|---|---|---|
| ink-900 on paper-0 (body text, light) | **17.64:1** | AAA |
| ink-600 on paper-0 (secondary text) | **8.42:1** | AAA |
| ink-500 on paper-0 (muted text) | **5.73:1** | AA (normal) |
| ink-400 on paper-0 (icons/meaningful borders) | **3.72:1** | AA non-text / large-text only — never body text |
| accent-600 on paper-0 (links) | **7.67:1** | AAA |
| white on accent-600 (primary button) | **7.81:1** | AAA |
| white on accent-700 (button hover) | **10.53:1** | AAA |
| success-700 on paper-0 | **6.13:1** | AA+ |
| success-800 on success-100 (badge) | **7.69:1** | AAA |
| warning-800 on paper-0 | **5.82:1** | AA |
| warning-900 on warning-100 (badge) | **7.35:1** | AAA |
| danger-600 on paper-0 | **5.86:1** | AA |
| white on danger-600 (destructive button) | **5.97:1** | AA |
| danger-800 on danger-100 (badge) | **8.02:1** | AAA |
| info-700 on paper-0 | **6.91:1** | AA+ |
| ink-50-adjacent `#E7E9EC` on ink-950 (dark body text) | **15.88:1** | AAA |
| ink-300 on ink-950 (dark secondary) | **8.62:1** | AAA |
| ink-400 on ink-950 (dark muted) | **5.10:1** | AA |
| accent-300 on ink-900 (dark links) | **7.06:1** | AAA |
| ink-950 on accent-300 (dark primary button) | **7.59:1** | AAA |
| success-300 / warning-300 / danger-300 / info-300 on ink-900 | **9.45 / 10.41 / 7.45 / 8.07:1** | AAA |

Rules derived from the numbers:

- Minimum body-text token on light = ink-500; ink-400 and lighter are barred from text roles (lint-checked in the semantic mapping).
- ink-100/ink-200 hairlines are **decorative only** (1.49:1); any border that conveys state or component boundary required for operation (inputs, focus, error) uses ink-400 or stronger (≥3:1, WCAG 1.4.11).
- Every tenant/white-label accent ramp must reproduce this table ≥ AA before shipping (automated in the tokens build).

### 2.6 Semantic color tokens (tier 2, light theme excerpt)

```css
:root, [data-theme="light"] {
  --sc-color-bg-page:            var(--sc-color-paper-0);
  --sc-color-bg-surface:         var(--sc-color-paper-0);
  --sc-color-bg-surface-raised:  var(--sc-color-paper-1);
  --sc-color-bg-surface-sunken:  var(--sc-color-paper-2);
  --sc-color-text-primary:       var(--sc-color-ink-900);
  --sc-color-text-secondary:     var(--sc-color-ink-600);
  --sc-color-text-muted:         var(--sc-color-ink-500);
  --sc-color-text-disabled:      var(--sc-color-ink-300);
  --sc-color-text-inverse:       var(--sc-color-paper-0);
  --sc-color-action-primary:     var(--sc-color-accent-600);
  --sc-color-action-primary-hover:  var(--sc-color-accent-700);
  --sc-color-action-primary-active: var(--sc-color-accent-800);
  --sc-color-link:               var(--sc-color-accent-600);
  --sc-color-border-decorative:  var(--sc-color-ink-200);
  --sc-color-border-strong:      var(--sc-color-ink-400);
  --sc-color-focus-ring:         var(--sc-color-accent-600);
  --sc-color-status-success:     #1B6E45;  /* + -bg / -on-bg variants */
  --sc-color-status-warning:     #8A5A00;
  --sc-color-status-danger:      #BA2E2E;
  --sc-color-status-info:        #0B5E8A;
}
[data-theme="dark"] {
  --sc-color-bg-page:            var(--sc-color-ink-950);
  --sc-color-bg-surface:         var(--sc-color-ink-900);
  --sc-color-bg-surface-raised:  var(--sc-color-ink-800);
  --sc-color-text-primary:       #E7E9EC;
  --sc-color-text-secondary:     var(--sc-color-ink-300);
  --sc-color-text-muted:         var(--sc-color-ink-400);
  --sc-color-action-primary:     var(--sc-color-accent-300);
  --sc-color-link:               var(--sc-color-accent-300);
  --sc-color-border-decorative:  var(--sc-color-ink-700);
  --sc-color-border-strong:      var(--sc-color-ink-400);
  --sc-color-focus-ring:         var(--sc-color-accent-300);
  /* status → 300-series, per §2.3 */
}
```

---

## 3. Typography tokens

### 3.1 Families (three roles — see direction §1.3 for licensing)

```css
--sc-font-display: "Archivo", var(--sc-font-fallback-sans);       /* variable: wght 100–900, wdth 62–125 */
--sc-font-ui:      "Instrument Sans", var(--sc-font-fallback-sans); /* variable: wght 400–700 */
--sc-font-mono:    "IBM Plex Mono", ui-monospace, "SFMono-Regular", Menlo, Consolas, monospace;
--sc-font-fallback-sans: -apple-system, "Segoe UI", system-ui, Arial, sans-serif;
```

- Self-hosted woff2, Latin-Ext subset v1; `font-display: swap` + `size-adjust`-tuned `@font-face` fallbacks (`Arial` metric overrides) → CLS ≈ 0. One `font-display` declaration per face (audit §2.1 found contradictory declarations — regression-tested).
- zh-Hans phase: pair with "IBM Plex Sans SC" (display/UI fallback chain per `:lang(zh-Hans)`), subsetted per-page via the build.
- Mono is mandatory for: SKU codes, dioptre/Rx values, MOQ/lead-time figures, prices in tables, timestamps, annotations. `font-variant-numeric: tabular-nums` on all data tables.

### 3.2 Fluid type scale (clamp; 1.20 ratio UI / 1.333 display)

| Token | Value | Role |
|---|---|---|
| `--sc-text-2xs` | `0.6875rem` (11px, fixed) | Annotations, table meta (mono) |
| `--sc-text-xs` | `0.75rem` (12px, fixed) | Badges, captions |
| `--sc-text-sm` | `0.875rem` (14px, fixed) | Dense UI, table body (dashboard) |
| `--sc-text-md` | `clamp(0.9375rem, 0.9rem + 0.2vw, 1rem)` | Body / UI default (15→16px) |
| `--sc-text-lg` | `clamp(1.0625rem, 1rem + 0.35vw, 1.1875rem)` | Lead paragraphs (17→19px) |
| `--sc-text-xl` | `clamp(1.25rem, 1.15rem + 0.5vw, 1.5rem)` | H4 / card titles (20→24px) |
| `--sc-text-2xl` | `clamp(1.5rem, 1.3rem + 1vw, 2rem)` | H3 (24→32px) |
| `--sc-text-3xl` | `clamp(1.875rem, 1.5rem + 1.8vw, 2.75rem)` | H2 (30→44px) |
| `--sc-text-4xl` | `clamp(2.375rem, 1.8rem + 2.8vw, 3.75rem)` | H1 (38→60px) |
| `--sc-text-display` | `clamp(2.75rem, 2rem + 4vw, 5.25rem)` | Hero only (44→84px, Archivo Expanded) |

`rem`-based with viewport *interpolation* only — user font-size settings and zoom always scale text (fixes audit §6.2.3/§6.3.4 vw-typography failure). Fixed sizes never below 11px, and 11–12px only for non-essential meta.

### 3.3 Weights, line-heights, letter-spacing

```css
--sc-font-weight-regular: 400;  --sc-font-weight-medium: 500;
--sc-font-weight-semibold: 600; --sc-font-weight-bold: 700;

--sc-leading-tight: 1.15;   /* display */
--sc-leading-snug:  1.3;    /* headings */
--sc-leading-body:  1.55;   /* body copy */
--sc-leading-ui:    1.4;    /* controls, tables */

--sc-tracking-display: -0.015em;  /* large Archivo */
--sc-tracking-normal:  0;
--sc-tracking-caps:    0.06em;    /* uppercase labels/annotations */
--sc-tracking-mono:    0;
```

Uppercase annotation style (`2xs/xs + caps tracking + mono`) is a named mixin (`.sc-annotation`) — the single sanctioned "technical label" treatment.

---

## 4. Layout, space, and shape tokens

### 4.1 Spacing (4px base)

```css
--sc-space-0: 0;      --sc-space-1: 4px;   --sc-space-2: 8px;
--sc-space-3: 12px;   --sc-space-4: 16px;  --sc-space-5: 20px;
--sc-space-6: 24px;   --sc-space-8: 32px;  --sc-space-10: 40px;
--sc-space-12: 48px;  --sc-space-16: 64px; --sc-space-20: 80px;
--sc-space-24: 96px;  --sc-space-32: 128px;
```

Section rhythm on public pages: `space-16`–`space-32`. Dashboard rhythm: `space-4`–`space-6`. Off-scale values require a token addition, not a magic number.

### 4.2 Radii (restrained — instrument, not bubble)

```css
--sc-radius-none: 0;   --sc-radius-xs: 2px;  --sc-radius-sm: 4px;
--sc-radius-md: 6px;   --sc-radius-lg: 8px;  --sc-radius-full: 9999px;
```

Controls (buttons/inputs): `sm`. Cards/dialogs: `md`. Overlays/drawers: `lg` max. `full` only for Avatar, Switch, count-dot. **No radius above 8px anywhere** (guardrail vs. rounded-2xl SaaS look).

### 4.3 Elevation / shadow (borders first, shadows for true overlays)

| Token | Value | Use |
|---|---|---|
| `--sc-shadow-0` | `none` | Default; cards use hairline borders, not shadows |
| `--sc-shadow-1` | `0 1px 2px rgb(12 14 18 / 0.06)` | Sticky header, subtle lift |
| `--sc-shadow-2` | `0 2px 8px rgb(12 14 18 / 0.10)` | Dropdown, popover, tooltip |
| `--sc-shadow-3` | `0 8px 24px rgb(12 14 18 / 0.14)` | Dialog, drawer |
| `--sc-shadow-4` | `0 16px 48px rgb(12 14 18 / 0.18)` | Command palette, largest overlays |

Dark theme: shadows near-invisible → overlays additionally raise surface color (`ink-800`) + 1px `ink-700` border.

### 4.4 Border / hairline system

```css
--sc-border-hairline: 1px solid var(--sc-color-border-decorative); /* structure, dividers */
--sc-border-strong:   1px solid var(--sc-color-border-strong);     /* inputs, meaningful boundaries (≥3:1) */
--sc-border-selected: 2px solid var(--sc-color-action-primary);
--sc-border-error:    1px solid var(--sc-color-status-danger);     /* + always icon/text, never color alone */
```

Hairlines are the system's primary structural device (direction §1.2). On ≥2x displays, table-internal rules may use `0.5px` equivalents via `box-shadow` — decorative contexts only.

### 4.5 Z-index scale

```css
--sc-z-base: 0;        --sc-z-raised: 10;    --sc-z-sticky: 100;
--sc-z-header: 200;    --sc-z-dropdown: 1000; --sc-z-overlay: 1300;
--sc-z-modal: 1400;    --sc-z-popover: 1500;  --sc-z-toast: 1600;
--sc-z-tooltip: 1700;
```

Arbitrary z-index values in component CSS are a lint error.

### 4.6 Motion tokens

```css
--sc-motion-duration-instant: 80ms;   /* hover/press feedback */
--sc-motion-duration-fast:    120ms;  /* micro-transitions, focus reticle */
--sc-motion-duration-base:    160ms;  /* expand/collapse, tab indicator */
--sc-motion-duration-slow:    240ms;  /* dialog/drawer enter */
--sc-motion-duration-story:   400ms;  /* reserved: brand moments only */

--sc-motion-ease-standard:   cubic-bezier(0.2, 0, 0, 1);    /* enter/move */
--sc-motion-ease-exit:       cubic-bezier(0.3, 0, 1, 1);    /* leave */
--sc-motion-ease-instrument: cubic-bezier(0.5, 0, 0.1, 1);  /* damped, no overshoot — signature */
```

**Reduced-motion policy (global, token-level):**

```css
@media (prefers-reduced-motion: reduce) {
  :root {
    --sc-motion-duration-instant: 0ms; --sc-motion-duration-fast: 0ms;
    --sc-motion-duration-base: 0ms;    --sc-motion-duration-slow: 0ms;
    --sc-motion-duration-story: 0ms;
  }
}
```

Plus: Motion-for-React components read a shared `useReducedMotion()` and render final states; calibration-tick and any autoplaying micro-animation are removed (not just shortened). CSS handles ~95% of motion; Motion for React only for overlay choreography, toast stacking, and animated reorder; GSAP not in the base system (direction §4.1).

### 4.7 Breakpoints + container queries

```css
--sc-bp-sm: 480px;  --sc-bp-md: 768px;  --sc-bp-lg: 1024px;
--sc-bp-xl: 1280px; --sc-bp-2xl: 1536px;
```

- Media queries handle **page scaffolding** (nav collapse, facet panel → drawer, grid column count).
- **Container queries handle components**: ProductCard, StatCard, SpecTable, RFQCard declare `container-type: inline-size` on their slot and adapt internally (e.g. ProductCard stacks its MOQ/price/lead strip below `24rem`). Components must never assume viewport width — this is what lets one card work in a 4-up public grid and a dashboard side panel.
- One responsive codebase; no UA sniffing (removes audit §2.1/§6.5.3 failure class).

### 4.8 Grid

| Context | Spec |
|---|---|
| Public web | 12-column CSS Grid; max content width `--sc-grid-max: 1320px`; gutter `--sc-space-6` (24px); page margin `--sc-space-4`→`--sc-space-10` fluid |
| Dashboard | Fluid full-width; 8px baseline; widget grid `repeat(auto-fill, minmax(280px, 1fr))` with `--sc-space-4` gaps; data tables full-bleed within panel padding `--sc-space-4` |
| Density | `data-density="comfortable|compact"` on the app shell: compact remaps row-height/padding component tokens (table row 44px → 36px, keeping ≥24px targets) |

---

## 5. Interaction-state matrix

Applies to every interactive component; component specs may extend, never remove, states.

| State | Visual spec | Rules |
|---|---|---|
| **Default** | Component tokens at rest | Cursor `pointer` only on actual actions |
| **Hover** | Bg/border shifts one ramp step (e.g. accent-600→700; wash `accent-50` on quiet controls); duration `instant` | Never the only affordance (touch has no hover) |
| **Active/pressed** | One further ramp step (accent-800); optional 1px translate suppressed under reduced motion | ≤`instant` duration |
| **Focus-visible** | Focus ring spec below | **Never** `outline: none` without replacement; focus not obscured by sticky UI (WCAG 2.4.11) |
| **Disabled** | `text-disabled` + `paper-1`/`ink-800` bg; **no opacity fades on text** (contrast-unsafe) | Keep visible focus exclusion; prefer enabled-with-validation over disabled submit where feasible; disabled controls get `aria-disabled` + tooltip explaining why when non-obvious |
| **Loading** | Inline Spinner replaces label/icon, width preserved (no layout shift); control gets `aria-busy="true"`, action locked | Skeleton for content, Spinner for actions; loading regions announce via `aria-live="polite"` (fixes audit §6.3.3 silent loading) |
| **Error** | `border-error` + error text (`danger-600`) + icon; `aria-invalid="true"` + `aria-describedby` → message id | Color never the sole indicator; message persists until corrected |

**Focus-ring specification (system-wide, one implementation):**

```css
.sc-focus-visible {
  outline: 2px solid var(--sc-color-focus-ring);
  outline-offset: 2px;
  border-radius: inherit;
}
```

- Light: accent-600 ring (7.67:1 vs paper — passes 3:1 non-text with margin). Dark: accent-300. On accent-colored surfaces: ring + 2px paper inner offset (double-ring) to guarantee ≥3:1 against any adjacent color.
- Applied via `:focus-visible` (keyboard/AT), not `:focus` — no ring flash on mouse click, full ring for keyboard users (fixes audit §6.3.6 "no visible focus strategy").
- The 120 ms "reticle" corner-tick embellishment is additive and disappears under reduced motion; the static ring always remains.

**Target size:** all interactive targets ≥24×24 px CSS (WCAG 2.5.8), ≥44×44 on primary mobile actions; where a visual glyph is smaller (table row chevron), the hit area is padded to compliance.

---

## 6. Component inventory

Legend: each entry = purpose · **key a11y requirement**. All components: tokens-only styling, CSS Module per component, full §5 state matrix, RTL-safe logical properties.

### 6.1 Primitives

| Component | Purpose | Key a11y requirement |
|---|---|---|
| Button (primary / secondary / quiet / destructive / icon) | Single action trigger; one primary per view | Real `<button>`; icon-only variant requires `aria-label`; loading state sets `aria-busy` and blocks double-submit |
| Input | Single-line text entry | Programmatic `<label>` always (no placeholder-as-label); error via `aria-invalid` + `aria-describedby` |
| Select | Native-first choice from ≤ ~15 options | Use native `<select>` where possible; custom version follows APG listbox pattern with full keyboard support |
| Combobox | Filterable choice / async lookup (suppliers, ports, HS codes) | APG combobox: `aria-expanded`, `aria-activedescendant`, result count announced via live region |
| Checkbox | Boolean / multi-select in facets | Native input visually restyled; indeterminate state exposed via `aria-checked="mixed"` |
| Radio | Single choice from visible set | `fieldset`/`legend` grouping; roving arrow-key navigation |
| Switch | Immediate on/off setting (not form submission) | `role="switch"` + `aria-checked`; label states both what and current effect |
| Textarea | Multi-line entry (RFQ notes, messages) | Auto-grow without scroll traps; character count via polite live region |
| Badge | Status descriptor (order state, stock) | Never color-only — always text; ≥4.5:1 text-on-subtle pairs (§2.5) |
| Tag | Removable filter/metadata chip | Remove button is a real button ("Remove filter: CR-39"), ≥24px target |
| Tooltip | Supplementary hint on hover/focus | Never sole carrier of required info; opens on focus, dismissible with Esc (WCAG 1.4.13); `aria-describedby` linkage |
| Avatar | Person/org visual identity | Meaningful alt (org/person name) or `aria-hidden` when adjacent text names it |
| Skeleton | Content-shaped loading placeholder | Container `aria-busy`; no per-bone announcements; shimmer removed under reduced motion |
| Spinner | Action-scale busy indicator | `role="status"` + visually-hidden label ("Loading quotes…") |
| Link | Navigation (vs Button = action) | Underlined in body copy (not color-only); external links announced |
| Divider / Rule | Hairline structure | `role="separator"` only when semantic; otherwise presentational |
| VisuallyHidden | AT-only text utility | Standard clip pattern; never `display:none` |

### 6.2 Composites

| Component | Purpose | Key a11y requirement |
|---|---|---|
| Card system (base / interactive / stat) | Bordered content container (hairline, not shadow) | Interactive card = one wrapping link with pseudo-content hit area; inner actions remain independently focusable |
| Table / DataGrid | The system's flagship: specs, orders, quotes | Real `<table>` + `<th scope>`, `<caption>`; sortable headers are buttons with `aria-sort`; sticky header must not obscure focused rows (WCAG 2.4.11); horizontal scroll region keyboard-focusable with `role="region"` + label |
| Tabs | Peer-view switching | APG tabs: arrow-key navigation, `aria-selected`, panel labelled by tab |
| Accordion | Progressive disclosure (FAQ, facet groups) | Trigger = `<button>` with `aria-expanded`/`aria-controls`; content in DOM (SEO + find-in-page) |
| Dialog | Blocking decision/task overlay | Native `<dialog>` or APG modal: focus trap, Esc close, focus return to invoker, `aria-labelledby` |
| Drawer | Side-panel tasks (filters mobile, quick view, cart) | Same modal contract as Dialog; swipe-close has button equivalent |
| Toast | Transient outcome notification | `role="status"` (polite) / `role="alert"` (errors only); pause on hover/focus; never the only record of an action's outcome; actions reachable without pointer |
| Pagination | Long-list navigation | `<nav aria-label="Pagination">`; current page `aria-current="page"`; server-rendered links (SEO) |
| Breadcrumb | Hierarchy orientation (catalog depth) | `<nav aria-label="Breadcrumb">` + ordered list + `aria-current` |
| EmptyState | Zero-data guidance + next action | Heading hierarchy preserved; primary action is a real button/link |
| ErrorState | Failure explanation + recovery | `role="alert"` on entry; retry is keyboard-first; error text human-readable (no bare codes) |
| Stepper | Multi-step flows (RFQ wizard, verification, checkout) | List semantics with step states announced ("Step 2 of 4, current"); steps completed remain navigable |
| FileUpload | Docs upload (certs, business licenses, logo artwork) | Real `<input type="file">` core; drag-drop is enhancement only; per-file progress + errors in a live region |
| DateInput / DateRangePicker | Lead-time windows, report ranges | Typed entry always possible (calendar is enhancement); APG grid pattern in calendar; localized formats |
| SearchField | Query entry with clear affordance | `type="search"` + labelled clear button; submits on Enter |
| Popover / Menu | Contextual action lists | APG menu pattern; Esc closes; focus returns to trigger |
| CommandPalette (v2) | Keyboard-first global navigation | Combobox pattern; shortcuts documented and remappable |

### 6.3 Marketplace-specific

| Component | Purpose | Key a11y requirement |
|---|---|---|
| ProductCard | Catalog unit: photo, name + mono SKU, MOQ/tier-price/lead strip | One accessible name for the card link (product name, not SKU); image alt = product descriptor; wishlist toggle separately focusable with state announced |
| SupplierCard | Directory unit: name, verification, capabilities, origin | Verification status in accessible name ("Meridian Optics — Verified supplier"); flag/origin icons have text equivalents |
| VerifiedBadge | The platform's trust mark (spectral micro-edge — whitelisted) | Text + icon (never icon-only); tooltip/dialog explains criteria + verification date; revoked state visually and programmatically distinct |
| SpecTable | Product technical readout (measurements, Rx ranges, coatings) | Real table semantics; units in header cells (`Lens width (mm)`); abbreviations expanded once (`SPH — sphere`) |
| FacetPanel | Faceted search: index, material, coating, SPH/CYL, MOQ, lead time | Grouped `fieldset`/`legend`; result-count updates announced politely; "clear all" first in focus order; mobile drawer keeps identical semantics |
| SearchBar (autocomplete) | Global product/supplier search | APG combobox; suggestion count announced; recent-search deletion keyboard-operable |
| RFQCard | RFQ summary: status, quantity, spec digest, deadline, quote count | Status Badge + deadline as text; whole-card link with independent inner actions |
| QuotationComparisonTable | Side-by-side quote evaluation (price, MOQ, lead, terms) | Column headers = supplier names (`<th scope="col">`); "best value" highlight duplicated as text marker, not color/gradient alone; horizontal scroll keyboard-accessible |
| PriceTierTable | Tiered wholesale pricing (fixes audit §6.1.3 hidden mechanics) | Table semantics; active tier for entered quantity announced ("120 units → $2.10/unit tier") |
| MOQIndicator | Minimum order quantity, surfaced at card + detail | Abbreviation expanded on first use / accessible name = "Minimum order quantity: 6 units" |
| LeadTimeIndicator | Production + shipping estimate | Absolute text ("12–15 days"), not visual bar alone; source/date of estimate available |
| CertificationChips | CE / FDA / ISO cert links on products & suppliers | Each chip links to the actual document/registry entry; expired certs marked in text |
| MessageThread | Buyer↔supplier conversation | `aria-live="polite"` for incoming (throttled); day separators as headings; attachments labelled with type + size |
| ConversationList | Inbox of threads | Unread state as text ("3 unread"), not dot-only; keyboard row navigation |
| NotificationCenter | Aggregated platform events | `aria-live` on badge count updates (polite, batched); mark-read operable per-item and bulk; panel follows menu/dialog semantics |
| CurrencySelector | Display-currency choice | Announces effect ("Prices shown in EUR — estimates; invoiced in USD"); persisted server-side |
| LocaleSwitcher | Language selection (en/fr/zh-Hans/de) | Options in their own language + `lang`/`hreflang` attributes (fixes audit §6.3.7 lang misattribution) |
| OrgSwitcher | Multi-org account context switch | Current org in accessible name; switch confirmation announced; keyboard menu pattern |
| StatCard / dashboard widgets | KPI display: value, delta, spark trend | Value + delta as text ("Quotes received: 14, up 3 this week"); sparkline `aria-hidden` with text equivalent; loading via Skeleton + `aria-busy` |

---

## 7. Accessibility requirements (baked in, WCAG 2.2 AA)

These are system-level defaults — components inherit them; audit §6.3 failures are called out as regression tests.

1. **Zoom & reflow:** no `user-scalable=no`, no `maximum-scale` (regression test against audit §6.3.1); layouts reflow to 320px width at 400% zoom (1.4.10); rem-based type only (§3.2).
2. **Focus:** global `:focus-visible` ring (§5); skip-to-content link first in DOM; focus never obscured by sticky header/toolbars (2.4.11); focus order follows visual order.
3. **Contrast:** text ≥4.5:1, large text/graphics ≥3:1, enforced by the verified pairs table (§2.5); tokens CI recomputes ratios on every change.
4. **Target size:** ≥24×24 CSS px minimum (2.5.8) audited in Storybook; dense-table exception uses padded hit areas.
5. **Motion:** `prefers-reduced-motion` collapses all durations at token level (§4.6); no autoplay video; no content flashing >3/s; no scroll-jacking.
6. **Forms & validation:** visible labels always; errors identified in text + icon + programmatically (3.3.1); suggestions provided (3.3.3); no re-entry of already-provided data in multi-step flows (3.3.7 redundant entry); accessible authentication — no cognitive puzzles, paste allowed in password/OTP fields (3.3.8).
7. **Dragging:** any drag interaction (FileUpload, dashboard widget arrangement) has a single-pointer, non-drag alternative (2.5.7).
8. **Live regions:** loading, async results, toasts, and message arrival announce politely (fixes audit §6.3.3); alerts reserved for errors.
9. **Language:** `lang` on `<html>` per locale, `lang` on inline foreign phrases; no client-side DOM machine translation (audit §6.3.7 removed by architecture).
10. **Structure:** landmarks (`header/nav/main/footer` + labelled `aside`); one `<h1>` per page; content available without JavaScript for public pages (SSR — fixes audit §6.3.2).
11. Accessibility statement page ships at `/accessibility` (audit §3 gap) and states the conformance target + feedback channel.

---

## 8. Storybook plan

- **Coverage rule:** every component ships with stories for **each state in §5** (default/hover/active/focus-visible/disabled/loading/error) plus: light + dark theme, comfortable + compact density (where applicable), RTL smoke story, long-content/overflow story (German strings + 40-character SKU codes), and empty/zero-data story for data components.
- **Interaction tests:** `@storybook/test` play-functions for keyboard paths (tab order, Esc-close, arrow-key patterns) on all APG-pattern components (Combobox, Tabs, Dialog, Menu, DataGrid).
- **A11y addon:** `@storybook/addon-a11y` (axe-core) runs per story; CI gate via test-runner — axe violations fail the build (rule exceptions require an inline justification comment reviewed by the DS owner).
- **Visual regression:** Chromatic (or Playwright screenshot equivalent) on the **critical set**: Button, Input, Select/Combobox, Badge, Table/DataGrid, Dialog, Toast, ProductCard, SupplierCard, VerifiedBadge, SpecTable, PriceTierTable, QuotationComparisonTable, FacetPanel, StatCard — light + dark, both densities, reduced-motion variant of animated states. Non-critical components get visual regression on their default story only.
- **Docs:** autodocs + a usage "Do / Don't" section per component (the guardrails from `design-direction.md` §4.3 rendered as visual anti-examples); tokens page auto-generated from `tokens.json`.
- Storybook is the **contract of record**: a component state that isn't in Storybook doesn't exist.

## 9. Definition of Done — design-system component

A component may be released (semver minor) only when all boxes tick:

- [ ] **API reviewed** — props named per system conventions; controlled + uncontrolled where applicable; `ref` forwarded; TypeScript types exported.
- [ ] **Tokens only** — zero raw color/size/z/duration literals (Stylelint passes); consumes semantic tier; works under `data-theme="dark"` and both densities without component-level overrides.
- [ ] **States complete** — all §5 states implemented and visible in Storybook.
- [ ] **Keyboard complete** — full operation per relevant APG pattern; documented key map in the story docs.
- [ ] **Screen-reader pass** — manual check in NVDA + VoiceOver for name/role/value/state on the component's primary flows.
- [ ] **axe clean** — a11y addon zero violations (or documented, owner-approved exceptions).
- [ ] **Target size** — every interactive target ≥24×24 px verified.
- [ ] **Reduced motion** — animated behavior verified under `prefers-reduced-motion: reduce`.
- [ ] **Responsiveness** — container-query behavior verified at narrow/wide slots; no viewport assumptions.
- [ ] **i18n-safe** — no concatenated sentence fragments; handles 1.5× text expansion; logical properties (RTL) used.
- [ ] **Performance** — no layout thrash on state change (loading preserves dimensions); bundle impact reported if >3 KB gz.
- [ ] **Visual regression** — baseline snapshots approved (critical set: full matrix per §8).
- [ ] **Docs** — usage guidance + Do/Don't published; changelog entry written.
- [ ] **Guardrail check** — no spectral-gradient usage outside the whitelist; no radius >8px; no shadow-as-border.

---

*End of design-system v0.1.0 specification. Next revision after the first three primitives (Button, Input, focus system) are implemented and the token build pipeline is live.*
