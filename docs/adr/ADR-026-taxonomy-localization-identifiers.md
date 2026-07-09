# ADR-026 — Taxonomy localization & stable-identifier policy

**Status:** Accepted — 2026-07-09 · Phase 8
**Refs:** [ADR-020](ADR-020-multilingual-content-model.md), [ADR-022](ADR-022-international-seo-mt-policy.md),
[supported-locales.md](../architecture/supported-locales.md), [optical-taxonomy-domain-model.md](../architecture/optical-taxonomy-domain-model.md)

## Context

The taxonomy is the platform's shared vocabulary across all 20 launch locales. Two failure modes must be
structurally impossible: (a) logic that depends on a translated label ("if category name == 'Frame'"),
and (b) storing one language per column. Phase 7 established two distinct localization strategies; Phase 8
must pick the right one for taxonomy content, which — unlike Phase 7's fixed supplier-type codes — is
**created and edited at runtime by platform staff**.

## Decision

1. **Stable identifiers are the only comparison key.** Every concept has an immutable language-neutral
   identifier — `uuid` PK plus a human-stable `code` (UPPER_SNAKE ASCII); countries key on ISO alpha-2,
   units on their symbol. All references, joins, foreign keys, validation, and equality use `uuid`/`code`.
   **Never compare a translated label, an English name, or a Chinese name.** Codes are immutable once
   published; a rename is a new code + an alias (ADR-027), never an in-place edit of identity.

2. **All human-facing text is DB-managed translation content** (ADR-020 per-entity `_translation`
   tables), **not** UI message-file labels. This is the key divergence from Phase 7: supplier
   *type/capability/scope* codes are a small fixed developer vocabulary labelled in `messages/*/taxonomy.
   json`; Phase-8 taxonomy entities are an **open, admin-managed set** whose names cannot live in shipped
   message files (admins add categories/brands after deploy). Each translatable entity therefore carries
   the full ADR-020 lifecycle row: original preserved (`is_original`), `translation_status`
   (MISSING→…→APPROVED), `source_version` stale detection, `translation_source`, reviewer/approval, and
   the deterministic fallback chain (requested → entity original → `en` → untranslated). Only `is_original`
   or `APPROVED` rows are shown publicly.

3. **`en` is the canonical authoring locale** and the terminal fallback; seed data ships `en`
   `is_original=true APPROVED`. Other locales begin `MISSING` and are filled by admin/import — the schema
   supports all 20 with zero DDL (a new locale = new rows).

4. **Machine translation is never authoritative** (ADR-022): `MACHINE_TRANSLATED` rows never display as
   human-verified and never leak to public reads.

5. **The UI still uses message files for its own chrome** (labels, buttons, the taxonomy admin
   interface), via the existing `taxonomy` next-intl namespace and a new `taxonomyAdmin` namespace —
   these are UI strings, distinct from the DB-managed *content* names of registry entities.

## Consequences

- Impossible to accidentally branch on a label: domain code only sees codes/ids.
- Adding a language never touches schema; taxonomy content localizes exactly like supplier profiles.
- Two clearly-separated localization planes: **UI chrome** (message files) vs **registry content**
  (translation tables) — documented so future modules pick correctly.

## Alternatives considered

| Option | Verdict |
|---|---|
| Translation tables for content + codes for identity (chosen) | Matches ADR-020; runtime-extensible; label-independent logic |
| Message-file labels for taxonomy entities (Phase-7 style) | Rejected: admin-created entities can't be in shipped bundles |
| `name_en`/`name_zh` columns | Rejected: ADR-020 STOP condition (schema churn per language) |
| Universal EAV translation table | Rejected: loses context ownership, weak typing (ADR-020) |
