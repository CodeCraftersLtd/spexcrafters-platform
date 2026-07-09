# ADR-025 — Attribute registry, data types, specification templates & validation engine

**Status:** Accepted — 2026-07-09 · Phase 8
**Refs:** [ADR-024](ADR-024-taxonomy-architecture.md), [optical-taxonomy-domain-model.md](../architecture/optical-taxonomy-domain-model.md)

## Context

Optical products carry heterogeneous specifications (frame eye-size in mm, lens index as a decimal,
coating as an enumeration, brand as a reference). These must come from **one master attribute registry**
so products never hardcode specs, and every attribute must declare validation, units, allowed values, and
search/comparison capability. Categories must define reusable specification layouts without duplication.
The registry must be extensible to new product families "without redesigning the database."

## Decision

1. **Master attribute registry.** Every specification field is one `taxonomy.attribute` row with a stable
   `uuid`/`code`, a `data_type`, optional `unit_code`/`enumeration_id`, validation bounds
   (`min_value/max_value/min_length/max_length/regex_pattern`), and capability flags
   (`searchable/filterable/sortable/comparable/facetable/seo/visible/deprecated`). Products (future) hold
   `(attribute_id → value)` pairs; they never define attributes inline.

2. **Data types are a closed, engine-coded primitive set** (`AttributeDataType` enum + DB `CHECK`):
   STRING, INTEGER, DECIMAL, BOOLEAN, DATE, ENUMERATION, MEASUREMENT, RANGE, JSON, REFERENCE,
   MULTI_SELECT, SINGLE_SELECT, FILE_REFERENCE, COLOR, COUNTRY, LANGUAGE, BRAND, CERTIFICATION. Each has
   defined validation/parse semantics in the engine. **"Extensibility without schema redesign" means the
   open sets — attributes, enumerations, values, categories, templates — are runtime-managed with no
   DDL; the primitive vocabulary is deliberately closed** because a genuinely new primitive requires
   engine code anyway. Adding one is a reviewed platform change (enum value + `CHECK` alter), not routine.

3. **Specification templates with inheritance.** Each category owns one `specification_template`; the
   **effective** template for a category is its own template attributes **plus those inherited from
   ancestor categories**, resolved at read time in the service. Layouts are never duplicated down the
   tree. `specification_template_attribute` carries the per-context `required` flag, an optional
   `conditional` rule (`requiredWhen`), a `default_value`, and `sort_order`.

4. **Validation engine (`SpecificationValidator`).** A pure service: `(categoryCode, Map<attributeCode,
   value>) → List<SpecificationViolation>`. It enforces required/conditional, data-type parse, numeric
   range, string length, regex, enumeration membership, reference existence, and rejects unknown
   attributes. Violation `code`s map to frontend i18n keys (problem+json `errors[]`). It is shipped with
   exhaustive unit tests and one demonstration endpoint (`POST /taxonomy/specifications/validate`). It
   validates values against templates; **it stores no product values** (products are a STOP condition).

5. **Search/comparison is metadata only.** The capability flags are recorded and exposed for a future
   Search module to build index/facet config from the registry. Phase 8 implements no search.

6. **JSON usage is bounded.** Only `attribute.ai_metadata` (future AI hints, opaque to the engine) and
   `specification_template_attribute.conditional` (engine-interpreted rule) are `jsonb`, mapped in JPA via
   `@JdbcTypeCode(SqlTypes.JSON)` over a `String`, following the V3 `audit_log.detail` precedent.

## Consequences

- Products reference attributes by id; specs are consistent, localizable, and validatable platform-wide.
- New optical families are added by creating categories/attributes/templates at runtime — no schema
  change — satisfying the extensibility mandate while keeping validation type-safe.
- A closed data-type set is an explicit, documented boundary rather than an open EAV free-for-all.

## Alternatives considered

| Option | Verdict |
|---|---|
| Attribute registry + closed data types (chosen) | Type-safe, validatable, runtime-extensible where it matters |
| Free-form EAV (any key/value) | Rejected: no validation, no comparability, no facet integrity |
| Per-category attribute columns | Rejected: schema redesign per family — the exact anti-goal |
| Open, DB-driven data types | Rejected: dishonest — the engine must code each type's semantics anyway |
