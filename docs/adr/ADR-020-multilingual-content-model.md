# ADR-020 — Multilingual Database Content Model & Translation Lifecycle

**Status:** Accepted — 2026-07-09 · **Refs:** ADR-019, [localization-content-classification.md](../architecture/localization-content-classification.md)

## Context
Database-managed multilingual business content (supplier profiles/facilities now; catalog/CMS later) must preserve the original language, keep translations separate, versioned, reviewable, and auditable — without one-column-per-language proliferation (`name_en`, `name_fr`, …) and without a single universal EAV translation table.

## Decision
**Normalized, per-context translation tables.** Each translatable entity in a bounded context owns a sibling `_translation` table:
```
supplier_profile               (id, organization_id, original_locale, source_version, … non-translatable cols)
supplier_profile_translation   (id, profile_id FK, locale, <translatable fields>,
                                 translation_status, source_locale, source_version,
                                 translation_source, translator_user_id, reviewer_user_id,
                                 created_at, updated_at, approved_at, approved_by)
  UNIQUE (profile_id, locale)
```
- **Original language preserved:** the entity row carries `original_locale` and the author's content in the original-language translation row (or on the entity — see model doc); translations are additive rows and can never overwrite the original.
- **Stable identifiers, not labels:** structured taxonomy (supplier types, capabilities, verification scopes, evidence types) uses language-independent **codes**; display labels are translated in message resources (UI) — domain logic never depends on a translated string.
- **Ownership:** translations live in their bounded context (supplier translations in the supplier module; future catalog/CMS translations in theirs). No cross-context universal translation table.
- **Future languages:** a new locale = new rows, never new columns/tables. Adding `zh-TW` touches no schema.

## Translation lifecycle (status enum, per translation row)
`MISSING → DRAFT → MACHINE_TRANSLATED → HUMAN_REVIEWED → APPROVED` (+ `REJECTED`). Stored with `translation_source` (`HUMAN | MACHINE | IMPORT`), `source_version` (the entity's `source_version` at translation time), timestamps, and translator/reviewer ids.
- **Stale detection:** the entity has a monotonic `source_version` bumped on any change to a translatable source field. A translation whose `source_version` < the entity's current `source_version` is **stale**; policy marks it `needs re-review` and the reviewer/public UIs show a stale warning. Approved translations are **never silently** left attached to changed source content.
- **MACHINE_TRANSLATED is never displayed as human-verified** without explicit product policy; the public fallback UI labels non-approved content (ADR-022 / fallback policy).

## Fallback (content, §10)
requested-locale APPROVED translation → supplier original language → `en` APPROVED (if any) → explicit untranslated state. Legal/registration fields and evidence content are **never** machine-translated.

## Alternatives
- One-column-per-language: rejected (schema churn per language, sparse, unindexable lifecycle). **STOP condition** in the brief.
- Universal EAV translation table: rejected (loses context ownership, weak typing/constraints, cross-module coupling).

## Migration path
Adding a translatable field = one column on the `_translation` table. Extracting a context to a service keeps its translation table with it.
