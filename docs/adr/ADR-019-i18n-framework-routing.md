# ADR-019 — i18n Framework, Locale Routing & Negotiation

**Status:** Accepted — 2026-07-09 · **Refs:** [supported-locales.md](../architecture/supported-locales.md)

## Context
Phase 7 makes SpexCrafters global by architecture across 20 BCP-47 locales (3 RTL). The Phase-1..6 web app used a hand-rolled locale middleware with `en/zh-Hans/fr/de` and static message imports — adequate for 4 locales, not for 20 with ICU pluralization, per-namespace loading, RTL, and localized metadata.

## Decision
**Adopt `next-intl`** as the Next.js App Router i18n framework, with a centralized `SupportedLocale` registry.
- **Routing:** locale is the first path segment for every route (`/{locale}/…`), public and authenticated. `next-intl`'s App Router middleware handles negotiation + rewrites; it composes with our existing session/CSRF middleware (locale runs first, then the auth guard on the already-localized path).
- **Negotiation priority (§6):** (1) explicit URL locale → (2) persisted user preference (`sc_locale` cookie, and server-side user pref once set) → (3) `Accept-Language` on first visit → (4) `en`. Once a user explicitly selects a locale it is persisted and **not** overridden by `Accept-Language` on later visits.
- **Language ≠ country:** no IP geolocation for language; the selector is free choice.
- **Resolution:** server-side by default (Server Components read messages on the server); Client Components use `next-intl`'s provider with **only the namespaces they need** — never all 20 bundles, never all namespaces.
- **Type safety:** `en` is the source; a generated type from `messages/en/*` gives autocomplete + compile errors on missing keys.
- **ICU MessageFormat** for all interpolation, plurals, select, number/date skeletons.

## Alternatives
- **FormatJS/react-intl directly:** more boilerplate for App Router + no first-class locale routing; `next-intl` wraps FormatJS's ICU core, so we keep ICU without the wiring. Rejected as primary.
- **Minimal internal ICU abstraction:** we already have a hand-rolled one; it lacks routing, plural/type-safety, and would reinvent `next-intl`. Rejected (maintenance burden at 20 locales).
- **Keep the hand-rolled middleware:** doesn't scale to namespaced per-locale loading + RTL + metadata. Rejected.

## Advantages
Mature, App-Router-native, RSC-friendly, ICU built-in, per-namespace loading, localized metadata API, active maintenance. **Disadvantages/Risks:** a framework dependency on the view layer (mitigated: messages are plain JSON we own; migration path below), and Next-version coupling (mitigated: pinned, covered by the existing web build gate).

## Migration path
Messages are framework-agnostic JSON keyed by namespace; if `next-intl` is ever replaced, the resources and the `SupportedLocale` registry are portable and only the provider/hook layer changes. The existing `en/zh-Hans/fr/de` files migrate to `en/zh-CN/fr/de` + the 16 new dirs; `zh-Hans`→`zh-CN` alias redirect at the edge preserves any live links.

## Bundle/loading
Server resolves messages; client islands import namespaces explicitly. No locale bundle exceeds its namespace set. Fonts load per active script group (ADR-021).
