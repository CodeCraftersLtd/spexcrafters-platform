# SpexCrafters — Phase 0/1 Deliverables Index

**Platform:** SpexCrafters — Global B2B Optical Marketplace and Sourcing Platform
**Competitor (functional benchmark only):** OpticLeague (www.opticleague.com) — see root [CURRENT_SITE_AUDIT.md](../CURRENT_SITE_AUDIT.md)
**Status:** ✅ Deliverables approved (with brand correction of 2026-07-08) · Sprint 1 authorized — see [sprint-01.md](sprint-01.md)
**Brand correction record:** [brand-correction-report.md](brand-correction-report.md)

| # | Deliverable | Document |
|---|---|---|
| A | Executive summary (competitor business model, strengths, weaknesses, opportunity) | [discovery/competitor-analysis.md](discovery/competitor-analysis.md) §A |
| B | Competitor feature inventory (RETAIN/IMPROVE/REPLACE/REMOVE/ADD) | [discovery/competitor-analysis.md](discovery/competitor-analysis.md) §B |
| C | Product differentiation strategy | [discovery/differentiation-strategy.md](discovery/differentiation-strategy.md) |
| D | Proposed sitemap (public, buyer, supplier, admin) | [architecture/sitemap.md](architecture/sitemap.md) |
| E | User roles & permission matrix | [architecture/roles-and-permissions.md](architecture/roles-and-permissions.md) |
| F | User journeys (visitor, buyer, supplier, admin, cross-cutting) | [architecture/user-journeys.md](architecture/user-journeys.md) |
| G | System architecture (modular monolith, contexts, events) | [architecture/system-architecture.md](architecture/system-architecture.md) |
| H | Technology decision matrix (+ rejected tech, dependency governance) | [architecture/technology-decisions.md](architecture/technology-decisions.md) |
| I | Database domain model (entities, indexes, migration strategy) | [database/domain-model.md](database/domain-model.md) |
| J | API architecture (REST/OpenAPI, auth, errors, idempotency, rate limits) | [api/api-architecture.md](api/api-architecture.md) |
| K | Design direction — three concepts + recommendation ("MERIDIAN with a spectral signature", ratified) | [design-system/design-direction.md](design-system/design-direction.md) |
| L | Design system — token architecture (`--sc-*`) + component inventory | [design-system/design-system.md](design-system/design-system.md) |
| M | Implementation roadmap (phases, DoD, risks, metrics) | [roadmap.md](roadmap.md) |
| N | Repository structure (monorepo) | [repository-structure.md](repository-structure.md) |
| O | First implementation sprint (approved) | [sprint-01.md](sprint-01.md) |

## Fixed project identity (binding)

- The platform is **SpexCrafters**. Positioning: *the global B2B sourcing, discovery, verification, and procurement infrastructure for the optical industry.*
- **OpticLeague is a competitor**, used only to analyze publicly observable functionality, weaknesses, market opportunities, and benchmarks. Never "OpticLeague 2.0 / redesign / replacement / re-platforming". No competitor source code, text, imagery, branding, datasets, or visual identity is used.
- Design direction: **MERIDIAN with a spectral signature**, owned exclusively by SpexCrafters. Token prefix: `--sc-*`. Java package root: `com.spexcrafters`. Working domain (assumption pending confirmation): `spexcrafters.com`.

## Open questions

| ID | Question | Status / Blocks |
|---|---|---|
| OQ-1 | Brand & relationship to OpticLeague | **RESOLVED 2026-07-08:** SpexCrafters, independent platform; OpticLeague strictly competitor |
| OQ-2 | ERP consumption vs. migration | **OBSOLETE** — premised on a rebrand scenario that does not exist; no competitor ERP relationship |
| OQ-3 | Founding suppliers willing to be named/verified at launch | Open — Phase 4–5 content |
| OQ-4 | Which real metrics are approved for public trust displays (no fabricated numbers will ship) | Open — homepage content |
| OQ-5 | Any official trade-fair partnerships for Events co-branding | Open — Events section |
| OQ-6 | Editorial content licensing for any syndicated insights | Open — Insights |
| OQ-7 | Price-tier visibility policy: public / login-gated / verified-gated | Open — Phase 4 PDP (DTOs already carry per-tier visibility) |
| OQ-8 | Target cloud provider & production domain/email infrastructure (working assumption: `spexcrafters.com`) | Open — Terraform; Sprint 1 proceeds cloud-agnostically |

## Next steps

1. Execute [sprint-01.md](sprint-01.md) (authorized).
2. Formalize ADR-001…016 from deliverable H (ADR-001…006 due within Sprint 1).
3. Security threat-model workshop (STRIDE over the container diagram in G).
