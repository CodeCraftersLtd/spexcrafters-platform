# ADR-021 — RTL Strategy & Global Typography

**Status:** Accepted — 2026-07-09 · **Refs:** [supported-locales.md](../architecture/supported-locales.md), design-system docs

## Context
Arabic (`ar`), Persian (`fa`), Urdu (`ur`) are first-class RTL. The platform mixes RTL prose with LTR technical tokens (model codes, SKUs, dioptre/Rx notation, ISO codes, numbers). Ten scripts across 20 locales need correct rendering without a giant font payload.

## Decision — RTL
- **Direction at the document root:** the `[locale]` layout sets `<html lang={locale} dir={dir}>` where `dir` comes from the `SupportedLocale` registry (`rtl` for ar/fa/ur, else `ltr`). One switch, no separate RTL app.
- **CSS logical properties only** in components: `margin-inline`, `padding-inline`, `border-inline`, `inset-inline-*`, `text-align: start/end`. Physical `left/right/margin-left/...` are banned in component CSS (Stylelint rule added, mirroring the existing token-only rule). MERIDIAN tokens are already direction-neutral; component conventions updated.
- **Bidi isolation:** mixed-direction inline content (RTL sentence containing an LTR model code / Rx value) wraps LTR technical spans in `<bdi>` / `unicode-bidi: isolate` so the LTR run doesn't reorder. A `<TechnicalText>` primitive enforces this.
- **Never mirrored:** logos, product images, technical diagrams, numbers, Latin model codes, optical prescriptions. Directional-meaning icons (arrows, progress, breadcrumb chevrons) flip via logical CSS / `[dir=rtl]` transforms; brand/technical icons do not.
- DOM order = logical order (no visual-order hacks) so screen-reader and keyboard order stay correct under RTL.

## Decision — Typography
- **Script-grouped, per-locale font loading** (see registry table). Latin base is always loaded (mixed content); the active locale's script group is added. No single global font payload.
- **Noto family** for non-Latin coverage (Sans SC/KR/JP/Thai/Devanagari/Bengali/Arabic; Nastaliq Urdu for display). Self-hosted `woff2`, subset per script, `font-display: swap` with metric-adjusted fallbacks to hold CLS ≈ 0.
- **Per-script line-height/metrics** overrides (CJK, Thai, Devanagari, Bengali, Arabic need more leading than Latin) via a `[lang]`/script-group CSS layer.
- **Text-expansion safe:** no fixed-width text controls; buttons/labels/inputs size to content (German/Russian/French can be +35%). Covered by a Tier-1 `de` visual check.

## Alternatives
- Duplicate RTL stylesheet / separate app: rejected (STOP condition — RTL must not require separate architecture).
- One global Noto super-font: rejected (multi-MB LCP/CLS regression).

## Performance/a11y
LCP/CLS budgets preserved via subgrouped loading; `lang`/`dir` correctness drives screen-reader pronunciation and bidi. Reduced-motion and focus rules from the design system are unchanged.

## Migration path
New script = new font-group entry + subset; logical-property discipline means new RTL locales (e.g. Hebrew) need no layout rework.
