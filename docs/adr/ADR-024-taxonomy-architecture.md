# ADR-024 — Optical Taxonomy module & schema architecture

**Status:** Accepted — 2026-07-09 · Phase 8 (Optical Taxonomy & Specification Registry)
**Refs:** [ADR-001](ADR-001-modular-monolith.md) (modular monolith), [ADR-020](ADR-020-multilingual-content-model.md),
[optical-taxonomy-domain-model.md](../architecture/optical-taxonomy-domain-model.md)

## Context

Phase 8 builds the master Optical Taxonomy & Specification Registry: the permanent single source of
truth for all optical vocabulary (categories, attributes, enumerations, units, countries,
certifications, brands, specification templates). Every future module (product catalog, search, RFQ,
SEO, analytics, AI) must depend on this registry and none may duplicate its terminology. The brief
lists candidate bounded contexts — Taxonomy, Specification, Brand, Measurement, Certification, Country,
Units, Localization — but also warns: *"Do not create unnecessary modules."*

## Decision

**One new module, `taxonomy`** (`com.spexcrafters.taxonomy`, Maven `spexcrafters-taxonomy`), owning a
single Postgres schema `taxonomy`. It houses the classification tree, attribute registry, enumeration
registry, unit/measurement registry, country registry, certification registry, brand registry,
specification templates, and the validation engine.

**Why one module, not eight.** These concepts form one tightly-coupled aggregate cluster: a
specification template binds attributes to a category; an attribute binds a unit and/or an enumeration;
reference-typed attributes resolve against country/brand/certification. Splitting them into separate
ArchUnit-isolated modules would force every relationship through cross-module `api` plumbing and make
every future consumer depend on 5–8 modules instead of one coherent registry. The brief's final
principle — *"Products/Suppliers/Search/RFQ/SEO/Analytics/AI will reference taxonomy"* — describes **one
foundation**, so it is modelled as one. Cohesion and a single consumer dependency outweigh premature
context-splitting at the foundation layer.

**Extraction seams preserved (ADR-001).** Within the module, sub-domains are cleanly separated by
package and aggregate (category / attribute / enumeration / unit / country / certification / brand /
template) and each translatable entity keeps its own `_translation` table, so any sub-domain (most
likely `brand`) can later be extracted into its own module carrying its tables with it, exactly as
ADR-020 anticipates. This is a reversible decision.

**Module wiring** (per the established convention): package layout `taxonomy.{api,domain,infrastructure,
web}`; register in root `apps/api/pom.xml` `<modules>` + `<dependencyManagement>`, in `application/
pom.xml`, and in `architecture-tests/pom.xml` (test scope); add `taxonomy_internals_are_module_private`
and `taxonomy_uses_other_modules_only_via_their_api` to `ModuleArchitectureTest` and the package to
`MODULE_PACKAGES`. Dependencies: `shared-kernel`, `audit` (audit events), `platform-access` (admin
authorization). Migration `V5__optical_taxonomy_registry.sql` (V1–V4 immutable).

**Schema ownership.** The `taxonomy` module owns exactly the `taxonomy` schema (one module = one schema,
as in Phase 7). Countries, units, and certifications live in the `taxonomy` schema (not the Phase-7
`reference` schema, which the supplier context owns for its fixed structural codes) because in Phase 8
they are runtime-managed, localized registries, not fixed developer codes.

## Consequences

- Future modules depend on one module (`spexcrafters-taxonomy`) for all optical vocabulary.
- A large module; mitigated by strict intra-module package/aggregate separation and per-entity
  translation tables that keep extraction cheap.
- Admin authorization reuses `platform_access` (new capabilities `TAXONOMY_READ/WRITE`, `BRAND_APPROVE`),
  consistent with the reviewer model; no new authz mechanism.

## Alternatives considered

| Option | Verdict |
|---|---|
| One `taxonomy` module (chosen) | Cohesive, single consumer dependency, extraction seams kept |
| 3 modules (taxonomy / brand / reference) | Rejected for Phase 8: cross-module plumbing for a tightly-coupled aggregate cluster; brand extractable later if it grows |
| 8 modules (one per candidate context) | Rejected: violates "no unnecessary modules"; every consumer would depend on 8 |
| Fold into an existing module | Rejected: taxonomy is a first-class permanent foundation, not a sub-feature |
