# ADR-022 — International SEO & Machine-Translation Policy

**Status:** Accepted — 2026-07-09 · **Refs:** ADR-019, ADR-020

## Context
Localized public pages (the supplier-profile foundation now; catalog/content later) must be correctly indexable per locale without duplicate-content loops, and the platform must have a disciplined machine-translation policy so unverified content is never presented as authoritative.

## Decision — International SEO
- **Localized URLs:** every public page is `/{locale}/…`; each localized page **self-canonicalizes** (canonical = its own locale URL). We never canonicalize all languages to `en`.
- **hreflang clusters:** reciprocal `hreflang` links are emitted **only for locales that have an available indexable translation** of that page (for supplier profiles: locales with an `APPROVED` translation, plus the original language). `hreflang="x-default"` points to the `en` version (policy: en is the default entry).
- **Indexability gate:** a localized page is `index,follow` only when it has real approved content for that locale; otherwise it is `noindex,follow` (prevents indexing empty/machine-translated shells). No indexable page is generated from empty or unreviewed machine translation.
- **Metadata/OG/structured data** are localized via `next-intl`'s metadata API and resolved server-side; sitemap architecture is segmented and locale-aware (foundation only — full sitemaps ship with the public-web phase).
- Locale codes in `hreflang` use BCP-47 exactly (`en`, `zh-CN`, `ar`, …).

## Decision — Machine-Translation policy
- MT is **development-assist only** and always tracked via the translation lifecycle (`MACHINE_TRANSLATED` status, `translation_source=MACHINE`). It is **never** silently promoted to `APPROVED`.
- **Never machine-translate:** legal company name, registration/certificate numbers, technical codes, or evidence documents (authoritative content). These are non-translatable (classification E).
- Public display of `MACHINE_TRANSLATED` supplier content requires explicit product policy and a visible "machine-translated / not verified" indicator; by default the fallback policy (ADR-020) shows the original language with a fallback indicator rather than unreviewed MT.
- **No paid MT API is integrated in Phase 7.** A `TranslationProvider` port is defined (future: human vendor / MT provider / internal reviewer) with no implementation wired.

## Alternatives
- Canonical-to-English for all locales: rejected (delists localized pages; wrong signal). 
- Index everything including MT: rejected (thin/duplicate content penalty; misrepresents quality).

## Risks / migration
Empty-locale pages are `noindex` until real content exists — acceptable for a foundation. When a translation vendor lands, only the `TranslationProvider` implementation and a lifecycle transition are added; SEO rules are unchanged.
