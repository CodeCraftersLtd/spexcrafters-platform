# @spexcrafters/ui

Sprint-1 UI primitives for SpexCrafters — React 19 components skinned **exclusively** by
`--sc-*` design tokens (CSS Modules, WCAG 2.2 AA). Spec of record:
`docs/design-system/design-system.md`; the Definition of Done for every component is §9
of that document. Storybook is the contract of record (§8): a state that isn't in
Storybook doesn't exist.

## Components (v1)

| Component | Notes |
|---|---|
| `Button` | `variant: primary \| secondary \| quiet \| destructive`, `size: sm \| md`, `loading`; real `<button>` (`type="button"` default), `forwardRef`; loading = inline spinner over a space-keeping label (width preserved, `aria-busy`, action locked) |
| `Input` | `forwardRef`; `invalid` sets `aria-invalid` + error border token; uses `--sc-border-strong` (≥3:1 boundary) |
| `FormField` | `label`/`htmlFor`/`hint`/`error`; hint id `${htmlFor}-hint`, error id `${htmlFor}-error` with `role="alert"`; **automatically clones element children** to inject `aria-describedby` (error id first, merged with existing) and `aria-invalid` — guarded via `isValidElement`, so non-element children pass through untouched |
| `Alert` | `tone: info \| success \| warning \| danger`; `role="status"` (info/success) or `role="alert"` (warning/danger); icon-free v1 — tone carried by border + bg + title text using the §2.5 AA pairs |
| `Spinner` | `role="status"` + visually hidden `label`; rotation removed under reduced motion |
| `VisuallyHidden` | Standard clip pattern, never `display: none` |

## Usage

```tsx
import { Alert, Button, FormField, Input } from "@spexcrafters/ui";
// Once per app (e.g. app/layout.tsx):
import "@spexcrafters/design-tokens/css";

<FormField label="Company name" htmlFor="company" error={errors.company}>
  <Input id="company" />
</FormField>
<Button loading={submitting}>Request quotation</Button>
```

This package ships **source TypeScript** (`main: ./src/index.ts`) — there is no build
step this sprint. Next.js consumers must transpile it:

```js
// next.config.js
module.exports = { transpilePackages: ["@spexcrafters/ui"] };
```

`'use client'` policy: `Button` and `Input` (refs + internal event handling) are client
components; `FormField`, `Alert`, `Spinner`, `VisuallyHidden` are server-compatible.

## State matrix (design-system.md §5)

Every interactive component implements: default, hover (one ramp step), active (one
further step, 1px translate suppressed under reduced motion), `:focus-visible` (system
ring — never `outline: none` without replacement), disabled (token colors, **no opacity
fades on text**), loading (spinner replaces label, width preserved, `aria-busy="true"`,
double-submit blocked), error (`--sc-color-status-danger` border + text + `aria-invalid`
+ `aria-describedby` — never color alone).

## Guardrails (binding — design-direction.md §4.3)

- **Tokens only:** zero raw color/size/z/duration literals in component CSS. Allowed
  exceptions: `0`, `1px` hairlines, `currentColor`, and documented tier-3 component
  tokens (e.g. the Spinner's `1em`/`2px` ring).
- **No radius above 8px** anywhere (`--sc-radius-lg` is the ceiling; `full` only for the
  whitelisted round shapes).
- **Spectral gradient whitelist:** `--sc-gradient-spectral` may only appear in Logo,
  HomepageHero keyline, VerifiedBadge, celebration states, and OG templates.
  **Intentionally, no v1 component in this package consumes the gradient token.**
- No shadow-as-border, no theme branching in component CSS, RTL-safe logical properties.

## Development

```
pnpm --filter @spexcrafters/ui storybook   # Storybook 8 (react-vite), a11y addon, theme toolbar
pnpm --filter @spexcrafters/ui test        # vitest + testing-library (jsdom)
pnpm --filter @spexcrafters/ui typecheck
```

Stories cover each §5 state plus dark theme (`data-theme` toolbar) and long-content
stories (German strings + 40-character SKU codes) per §8. Hover/active are one-step token
shifts exercised by real interaction; visual-regression coverage of the full matrix lands
with the Chromatic sprint. Release checklist: design-system.md §9.
