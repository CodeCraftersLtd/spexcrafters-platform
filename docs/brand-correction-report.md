# Brand Correction Report — SpexHub → SpexCrafters

**Date:** 2026-07-08 · **Trigger:** stakeholder brand-identity correction (mandatory, project-wide)
**Binding identity:** Platform = **SpexCrafters** (Global B2B Optical Marketplace and Sourcing Platform) · **OpticLeague = competitor**, functional benchmark only · "SpexHub" retired · Prohibited framings: "OpticLeague 2.0 / Next Generation / redesign / replacement / re-platforming".

## 1. Pre-correction scan (whole repository)

| Term | Occurrences | Files |
|---|---|---|
| `SpexHub` / `spexhub` (incl. `spexhub.net`, `net.spexhub`) | 45 + domain/package uses | 15 docs files |
| `OpticLeague` / `opticleague` | 41 | `CURRENT_SITE_AUDIT.md` (18), `docs/**` (9), `.claude/settings.local.json` (14) |
| `--sx-` token prefix / `.sx-` class prefix / `` `sx` `` namespace | 127 | `docs/design-system/design-system.md`, `docs/design-system/design-direction.md` |
| `SpexCrafters` | 0 (pre-correction) | — |
| Prohibited framings ("OpticLeague redesign" etc.) | Framing present in `CURRENT_SITE_AUDIT.md` header/§5–§10; hedged "rebrand" contingency in OQ-1/OQ-2 references | 4 files |

## 2. Classification & disposition of every occurrence class

| Occurrence class | Classification | Action taken |
|---|---|---|
| "SpexHub" as platform name in all 15 docs (headers, prose, diagrams, journey copy, CTAs like "Sell on SpexHub") | **RENAME TO SPEXCRAFTERS** | Renamed (deliberate per-file edits, not blind global replace) |
| `spexhub.net` domains (`api.spexhub.net`, journeys intro, RFC-9457 problem URLs) | **RENAME TO SPEXCRAFTERS** | → `spexcrafters.com` / `api.spexcrafters.com` (working domain, flagged as assumption in OQ-8) |
| Java package root `net.spexhub.<context>` | **RENAME TO SPEXCRAFTERS** | → `com.spexcrafters.<context>` |
| Monorepo root `spexhub/` in repository-structure | **RENAME TO SPEXCRAFTERS** | → `spexcrafters/` |
| Design tokens `--sx-*` (125), class prefixes `.sx-*` (2), namespace note `` `sx` `` | **RENAME (token prefix)** | → `--sc-*`, `.sc-*`, `` `sc` `` = SpexCrafters namespace |
| Restricted gradient token `--sx-gradient-spectral` + whitelist/lint references | **RENAME (token prefix)** | → `--sc-gradient-spectral` (whitelist rules unchanged) |
| OpticLeague references in `docs/discovery/competitor-analysis.md` and other docs (explicitly competitor-framed) | **KEEP AS COMPETITOR REFERENCE** | Unchanged |
| `CURRENT_SITE_AUDIT.md` title/"Project: OpticLeague — Website Redesign" framing and body sections §5, §7–§10 written as redesign guidance | **REVIEW MANUALLY** | Header retitled "Competitor Technical Audit: OpticLeague"; binding reframing note added stating the audit is competitor benchmarking evidence and that redesign-framed sections are superseded by `docs/`. Body observations preserved as evidence (no blind rewrite). |
| OQ-1 ("independent brand vs. OpticLeague rebrand") in README, sprint-01, competitor-analysis naming note | **REMOVE (resolved)** | Marked RESOLVED; naming note rewritten as confirmed brand identity; rebrand contingencies deleted |
| OQ-2 (competitor-ERP consumption/migration) in README | **REMOVE (obsolete)** | Marked OBSOLETE — premised on a nonexistent rebrand scenario |
| Domain-model §I.5.5 "If OQ-1 resolves to OpticLeague rebrand → ETL from existing ERP" | **REMOVE (obsolete)** | Rewritten: no competitor data import, ever; integration-compatible schema retained for SpexCrafters' own future ERP integrations |
| Competitor-analysis B2.1 assumption "[no requirement to preserve competitor's live order flow — see OQ-1]" | **REMOVE (resolved)** | Rewritten as definitive statement of independence |
| `opticleague.com` entries in `.claude/settings.local.json` | **KEEP** | Tooling permission config for competitor research (WebFetch allowlist) — not project documentation |
| Prohibition statements quoting the banned phrases (README §identity, competitor-analysis brand note) | **KEEP** | They state the rule; intentionally retained |

## 3. Files modified

1. `CURRENT_SITE_AUDIT.md` — retitled; binding reframing note; competitor framing
2. `docs/README.md` — rewritten: SpexCrafters identity block, OQ-1 resolved / OQ-2 obsolete, status → approved/Sprint-1 authorized, link to this report
3. `docs/discovery/competitor-analysis.md` — project line, brand-identity note, all platform-name renames, B2.1 assumption removed
4. `docs/discovery/differentiation-strategy.md` — all platform-name renames
5. `docs/architecture/sitemap.md` — header rename
6. `docs/architecture/roles-and-permissions.md` — header rename
7. `docs/architecture/user-journeys.md` — all renames incl. "Sell on SpexCrafters" CTAs, intro domain → spexcrafters.com
8. `docs/architecture/system-architecture.md` — header, container-diagram label, package root `com.spexcrafters`
9. `docs/architecture/technology-decisions.md` — header rename
10. `docs/database/domain-model.md` — header; §I.5.5 rewritten (no competitor data import)
11. `docs/api/api-architecture.md` — header; base URL + problem-type URLs → `api.spexcrafters.com`
12. `docs/design-system/design-direction.md` — all platform renames; gradient token → `--sc-gradient-spectral`
13. `docs/design-system/design-system.md` — title; namespace note; 123× `--sx-` → `--sc-`; `.sx-focus-visible`/`.sx-annotation` → `.sc-*`
14. `docs/roadmap.md` — header rename
15. `docs/repository-structure.md` — header; monorepo root `spexcrafters/`
16. `docs/sprint-01.md` — header + status APPROVED; blocking-inputs list updated (brand + design direction resolved; domain assumption recorded)
17. `docs/brand-correction-report.md` — this report (new)

## 4. Post-correction verification

- `SpexHub|spexhub|SPEXHUB|--sx-|.sx-|net.spexhub` → **0 occurrences** repository-wide.
- Banned framings ("OpticLeague 2.0", "OpticLeague redesign", …) → occur only inside the prohibition statements themselves.
- `OpticLeague` remains only in competitor-identified contexts (audit + benchmarking docs + tool config).

## 5. Recorded assumptions (pending confirmation, tracked as OQ-8)

- Production domain: **`spexcrafters.com`** (and `api.spexcrafters.com`). If the actual domain differs, it is a config-level change (env vars, OpenAPI servers block, problem-type URIs) — no code architecture impact.
- Java package root: **`com.spexcrafters`**.
- npm scope for packages: **`@spexcrafters/*`**.
- Design-token namespace: **`sc`** (`--sc-*`), Stylelint rules and whitelist paths follow this prefix.
