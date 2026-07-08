# @spexcrafters/design-tokens

Design tokens for SpexCrafters ("MERIDIAN with a spectral signature"). The DTCG-format
JSON in `src/` is the **single source of truth**; `build/` contains generated artifacts.
Spec: `docs/design-system/design-system.md` (token values, verified contrast pairs) and
`docs/design-system/design-direction.md` §4 (direction + guardrails).

## Token tiers

| Tier | Prefix pattern | Answers | May reference |
|---|---|---|---|
| 1. Primitive | `--sc-<category>-<name>-<step>` (e.g. `--sc-color-ink-900`) | What values exist? | Raw values only |
| 2. Semantic | `--sc-<category>-<role>[-<variant>][-<state>]` (e.g. `--sc-color-text-primary`) | What is this value for? | Primitives only |
| 3. Component | `--sc-<component>-<property>[-<state>]` | What does this component consume? | Semantic (preferred) |

Rules (lint-enforced once Stylelint lands):

- Application CSS consumes **semantic and component tokens only**. Referencing a primitive
  (`--sc-color-ink-*`, `--sc-color-accent-*`, …) outside this package is an error.
- Theming re-maps **tier 2 only** via `data-theme="light" | "dark"` on `<html>`.
  Component CSS never branches on theme.
- No raw hex/px in component CSS Modules except `0`, `1px` hairlines, and `currentColor`.
- State suffixes are a closed set: `-hover | -active | -focus | -disabled | -error`.

## Naming

`--sc-{category}-{...}` — categories: `color | font | text | space | radius | shadow |
border | z | motion | size | grid | gradient` (plus `bp` for breakpoints). Kebab-case;
numeric steps for ramps (`ink-900`), t-shirt or numeric for scales (`space-4`, `text-lg`).

## Usage

```css
/* app root stylesheet (loaded once, before component CSS) */
@import "@spexcrafters/design-tokens/css";
```

```tsx
// Non-CSS consumers: charts, <canvas>, OG images, email templates
import { colors, space, text, motion, themes } from "@spexcrafters/design-tokens/ts";

ctx.fillStyle = colors.accent[600];
const darkBg = themes.dark.color.bg.page;
```

`tokens.css` provides:

- `:root` — all tier-1 primitives,
- `:root, [data-theme="light"]` / `[data-theme="dark"]` — tier-2 semantic mappings
  (+ `color-scheme` so native controls match),
- the global reduced-motion collapse (all `--sc-motion-duration-*` → `0ms`),
- the system focus ring (`:focus-visible` + opt-in `.sc-focus-visible`),
- the `.sc-annotation` mixin class (the single sanctioned technical-label treatment).

## Building

The build is a **zero-dependency Node script** (no Style Dictionary): a deterministic
depth-first traversal of the JSON sources in fixed key order.

```
pnpm --filter @spexcrafters/design-tokens build        # regenerate build/
pnpm --filter @spexcrafters/design-tokens build:check  # byte-compare; exit 1 on drift
```

**Bootstrap note (Sprint 1):** both the sources *and* the generated `build/` outputs are
committed. CI regenerates and diff-checks (`build:check`) so a hand-edited or stale
`build/` fails the pipeline. Never edit `build/` by hand.

Deviation from the spec's §1.3 sketch: dark-theme semantics are emitted as a
`[data-theme="dark"]` block inside the single `tokens.css` (no separate `tokens.dark.css`),
and `tokens.ts` is consumed as TypeScript source (no separate `.d.ts`).

## Restricted token — spectral gradient

`--sc-gradient-spectral` is whitelisted to exactly five surfaces (design-direction.md §4.1):

1. the logo mark's refracted edge,
2. the homepage hero's single keyline,
3. the VerifiedBadge micro-edge,
4. empty/celebration states,
5. marketing/social OG imagery.

**Nowhere else.** Decorative-only (never carries text or meaning), max 4px stroke/edge in
UI, max one instance per viewport, design-system-owner sign-off required. Enforced by a
Stylelint allowlist of module paths. No v1 component in `@spexcrafters/ui` consumes it.

## Semantic additions beyond the §2.6 excerpt

The spec's §2.6 block is an excerpt; this package adds the following tier-2 tokens, all
derived from the §2.5 verified pairs and the §5 state matrix: `--sc-color-bg-hover-wash`,
`--sc-color-bg-active-wash` (quiet-control washes), `--sc-color-text-placeholder`,
`--sc-color-action-danger[-hover|-active|-text]` (destructive actions),
`--sc-color-border-strong-hover`, and the `--sc-color-status-*-bg` / `-on-bg` variants.
Status ramps carry the six stops named in §2.3 tables (100/300/600/700/800/900); stops not
fixed by the doc were interpolated consistently with its values.

## Versioning

Semver, versioned like a library:

- **Major** — renaming or removing any token (a codemod ships with every major).
- **Minor** — adding tokens or components’ semantic mappings.
- **Patch** — value corrections that do not change token names or contrast class.

Tenant/white-label accent ramps must reproduce the §2.5 contrast table at ≥ AA before
shipping (automated check planned in this build).
