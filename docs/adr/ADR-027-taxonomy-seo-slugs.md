# ADR-027 — Taxonomy SEO slugs & alias resolution

**Status:** Accepted — 2026-07-09 · Phase 8
**Refs:** [ADR-019](ADR-019-i18n-framework-routing.md), [ADR-022](ADR-022-international-seo-mt-policy.md),
[ADR-026](ADR-026-taxonomy-localization-identifiers.md)

## Context

Future category landing pages and product URLs need stable, localized, SEO-friendly slugs. Slugs must be
unique (no collisions within a locale), survive taxonomy restructuring (renames/merges) without breaking
inbound links, and never become identity (identity is `code`/`uuid`, ADR-026). Phase 8 builds the slug
**foundation** only — no landing pages, no routing (those are later phases / STOP conditions).

## Decision

1. **Localized slugs in a dedicated table.** `taxonomy.category_slug(category_id, locale, slug, is_primary,
   active)` holds one **primary** active slug per (category, locale) plus retained historical rows.
   Uniqueness is enforced per locale by `uq_category_slug_locale UNIQUE(locale, slug)` — **duplicate slugs
   within a locale are impossible at the database level**. Slugs are ASCII-normalized, lowercase,
   hyphen-separated, `≤160` chars, generated from the locale's approved category name with a
   deterministic transliteration + de-duplication suffix on collision.

2. **Slugs are not identifiers.** Resolution maps slug → `category_id`; all logic then uses the code/uuid.
   A slug may change (rebrand, better SEO) without changing identity.

3. **Aliases = redirect history.** When a primary slug changes, the old row is kept `active=true,
   is_primary=false` so a future router can issue a 301 to the current primary. Category **code** aliases
   (`taxonomy.category_alias`) serve the same purpose at the identifier layer for restructuring/merges.

4. **Per-locale independence.** Each locale has its own slug set; the `ar/fa/ur` RTL locales and CJK
   locales get transliterated-or-authored slugs, never a shared Latin slug forced across languages.
   Unfilled locales fall back to the `en` slug for routing until authored (ADR-019 fallback).

5. **Foundation only.** Phase 8 stores, uniquely-constrains, generates, and exposes slugs (read API +
   admin edit). It builds **no** landing pages, sitemaps, or slug routing — those are later phases.
   Building marketplace/landing pages now is a STOP condition.

## Consequences

- Slug collisions are structurally prevented; restructuring never orphans inbound links (alias rows).
- Identity and URL are decoupled — URLs can evolve freely.
- The slug generator + uniqueness are unit/integration tested (slug-uniqueness tests) this phase.

## Alternatives considered

| Option | Verdict |
|---|---|
| Dedicated per-locale slug table with alias history (chosen) | DB-enforced uniqueness, redirect-safe, i18n-correct |
| `slug` column on the category row | Rejected: one language only; no alias/redirect history |
| Slug as the primary key / identity | Rejected: ADR-026 — labels/URLs are never identity |
| Global (locale-agnostic) slug uniqueness | Rejected: forces one language's slugs on all locales |
