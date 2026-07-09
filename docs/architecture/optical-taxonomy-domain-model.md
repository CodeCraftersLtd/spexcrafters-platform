# Optical Taxonomy & Specification Registry — Domain Model (Phase 8)

**Status:** Binding. This document is the single source of truth for the master Optical Taxonomy
& Specification Registry. Every future optical object in SpexCrafters (products, supplier claims,
search facets, RFQ line items, SEO landing pages, analytics dimensions, AI matching) **references
this registry by stable identifier**. No future module may invent optical vocabulary outside it.

Companion decisions: [ADR-024](../adr/ADR-024-taxonomy-architecture.md) (module & schema),
[ADR-025](../adr/ADR-025-attribute-specification-model.md) (attributes, data types, validation,
templates), [ADR-026](../adr/ADR-026-taxonomy-localization-identifiers.md) (localization &
identifier policy), [ADR-027](../adr/ADR-027-taxonomy-seo-slugs.md) (SEO slugs & aliases).
Reuses [ADR-020](../adr/ADR-020-multilingual-content-model.md) translation tables and
[supported-locales.md](supported-locales.md).

---

## 0. First principles (binding)

1. **Stable identifiers are the source of truth.** Every concept has an immutable, language-neutral
   identifier: a `uuid` primary key and a human-stable `code` (UPPER_SNAKE, ASCII). Countries use the
   ISO 3166-1 alpha-2 code as PK; units use their symbol code as PK.
2. **Display names are never the source of truth.** Logic, joins, comparisons, and references use
   `uuid`/`code` **only**. Never compare an English name, a Chinese name, or any translated label.
   Names live in `_translation` rows and are for humans.
3. **Everything is localizable.** Every human-facing name/description is a translatable field in a
   per-entity `_translation` table (ADR-020): original preserved, approved translation, translation
   status, `source_version` stale detection, deterministic `en` fallback. **Never one column per
   language.**
4. **Extensible without schema redesign.** New categories, attributes, enumerations, enumeration
   values, units, brands, certifications, and templates are created at runtime by platform staff — no
   DDL. The only closed set is the primitive **data-type** vocabulary (§4), which is engine-coded by
   design; adding a primitive is a deliberate platform change, not routine data entry.
5. **One bounded context, one schema.** All registry tables live in the `taxonomy` Postgres schema,
   owned by the `taxonomy` module (ADR-024). Cross-schema FKs into `identity`/`reference` are an
   integrity net only; cross-module Java access is ArchUnit-forbidden.

Conventions inherited from V1–V4 (see [V4 migration] header): `uuid` PKs (UUIDv7 from the
application), `timestamptz`, snake_case, string enums re-checked by DB `CHECK`, the audit tail
(`created_at, updated_at, created_by, updated_by, version`) on every business table, constraint
naming `ck_<table>_<field>` / `uq_<name>` / `ix_<table>_<cols>`, FK columns as raw `uuid`. **Migrations
V1–V4 are immutable; this phase adds `V5__optical_taxonomy_registry.sql` only.**

---

## 1. Translation-row shape (reused verbatim from ADR-020)

Every `*_translation` table in this registry uses the canonical lifecycle shape:

```
id                 uuid PRIMARY KEY
<parent>_id        uuid NOT NULL REFERENCES taxonomy.<parent>(id)   -- (country/unit use the code FK)
locale             varchar(16) NOT NULL REFERENCES reference.supported_locale(code)
<translatable text columns...>                                     -- name, description, label, display_name
translation_status varchar(32) NOT NULL                            -- MISSING|DRAFT|MACHINE_TRANSLATED|HUMAN_REVIEWED|APPROVED|REJECTED
source_locale      varchar(16) NOT NULL
source_version     int         NOT NULL
translation_source varchar(16) NOT NULL                            -- HUMAN|MACHINE|IMPORT
translator_user_id uuid
reviewer_user_id   uuid
approved_at        timestamptz
approved_by        uuid
is_original        boolean NOT NULL DEFAULT false
created_at, updated_at, created_by, updated_by, version            -- audit tail
CONSTRAINT uq_<parent>_translation UNIQUE (<parent>_id, locale)
CONSTRAINT ck_<parent>_translation_status CHECK (translation_status IN (...6 states...))
CONSTRAINT ck_<parent>_translation_source CHECK (translation_source IN ('HUMAN','MACHINE','IMPORT'))
INDEX ix_<parent>_translation_<parent> (<parent>_id) ; INDEX ix_<parent>_translation_locale (locale)
```

**Stale detection:** the parent row carries `source_version int NOT NULL DEFAULT 1`, bumped on any
change to a translatable source field. A non-original translation whose `source_version <` the parent's
current `source_version` is **stale** and flagged for re-review. **Fallback chain** (read services):
requested-locale APPROVED (or original) → entity original locale → `en` APPROVED → untranslated marker.
Only `is_original` or `APPROVED` rows are ever shown publicly (MACHINE_TRANSLATED/DRAFT never leak).

The registry's default authoring locale is `en` (`is_original=true`, seeded `APPROVED`). Backend reuses
the shared-kernel `SupportedLocale` enum and clones the `TranslationStatus`/`TranslationSource` enums
into the taxonomy context (ADR-020: translations live in their own bounded context).

---

## 2. Category tree (hierarchical taxonomy)

Unlimited-depth adjacency tree with a materialized ancestor `path` for subtree reads; supports
restructuring (re-parent), inactive nodes, alias codes, and localized SEO slugs.

### `taxonomy.category`
| column | type | notes |
|---|---|---|
| id | uuid PK | UUIDv7 |
| parent_id | uuid NULL → taxonomy.category(id) | NULL = root (e.g. `OPTICAL`, `MACHINERY`) |
| code | varchar(64) NOT NULL | **stable id**, UPPER_SNAKE, `UNIQUE` |
| depth | int NOT NULL | 0 = root; maintained by the service on create/reparent |
| path | varchar(1000) NOT NULL | `/`-joined ancestor codes incl. self, e.g. `/OPTICAL/LENS/PRESCRIPTION_LENS/` |
| classification | varchar(32) NOT NULL | high-level product family: `LENS,FRAME,SUNGLASSES,CONTACT_LENS,MACHINERY,LAB_EQUIPMENT,ACCESSORY,PACKAGING,COMPONENT,OTHER` (CHECK) |
| active | boolean NOT NULL DEFAULT true | inactive nodes hidden from public reads, kept for integrity |
| sort_order | int NOT NULL DEFAULT 0 | sibling ordering |
| source_version | int NOT NULL DEFAULT 1 | stale trigger for name/description |
| + audit tail | | |

Constraints: `uq_category_code UNIQUE(code)`, `ck_category_classification CHECK(...)`,
`ix_category_parent (parent_id)`, `ix_category_path (path)`. A `ck_category_no_self_parent CHECK
(parent_id IS NULL OR parent_id <> id)`. Cycle prevention is enforced in the service (path check).

### `taxonomy.category_translation`
Translatable: `name varchar(300)`, `description varchar(4000)`. Full lifecycle shape (§1).

### `taxonomy.category_alias`
| id uuid PK | category_id uuid NOT NULL FK | alias_code varchar(64) NOT NULL | + audit |
`uq_category_alias UNIQUE(alias_code)` — a global alias namespace so a retired/merged code still
resolves to its successor.

### `taxonomy.category_slug` (localized SEO — see ADR-027)
| id uuid PK | category_id uuid NOT NULL FK | locale varchar(16) NOT NULL FK | slug varchar(160) NOT NULL | is_primary boolean NOT NULL DEFAULT true | active boolean NOT NULL DEFAULT true | + audit |
Constraints: `uq_category_slug_locale UNIQUE(locale, slug)` (**no duplicate slug within a locale**),
`ix_category_slug_category (category_id)`, one primary per (category, locale) enforced in service.
Non-primary/inactive rows are historical aliases retained for 301 redirects.

---

## 3. Enumeration registry (reusable controlled vocabularies)

Shared, versionable enumerations referenced by attributes (data type `ENUMERATION`). Examples:
`FRAME_SHAPE, FRAME_MATERIAL, LENS_MATERIAL, LENS_DESIGN, LENS_COATING, LENS_INDEX,
PHOTOCHROMIC_COLOR, POLARIZATION, GENDER, AGE_GROUP, MACHINE_VOLTAGE, POWER_SUPPLY, PACKAGING_TYPE`.

### `taxonomy.enumeration`
| id uuid PK | code varchar(64) NOT NULL UNIQUE | description_key varchar(120) NULL | active boolean | + audit |

### `taxonomy.enumeration_value`
| id uuid PK | enumeration_id uuid NOT NULL FK | code varchar(64) NOT NULL | sort_order int | deprecated boolean NOT NULL DEFAULT false | active boolean NOT NULL DEFAULT true | + audit |
`uq_enumeration_value UNIQUE(enumeration_id, code)`, `ix_enumeration_value_enum (enumeration_id)`.

### `taxonomy.enumeration_value_translation`
Translatable: `label varchar(300)`, `description varchar(2000)`. Full lifecycle shape.

### `taxonomy.enumeration_value_alias`
| id uuid PK | enumeration_value_id uuid NOT NULL FK | alias varchar(120) NOT NULL | + audit |
`uq_enumeration_value_alias UNIQUE(enumeration_value_id, alias)` — import/synonym resolution.

---

## 4. Attribute registry & data types

Every technical specification field is one row in the master attribute registry. Products never
hardcode specs; they reference `attribute` definitions.

### Data-type vocabulary (Java enum `AttributeDataType` + DB `CHECK`) — closed set
`STRING, INTEGER, DECIMAL, BOOLEAN, DATE, ENUMERATION, MEASUREMENT, RANGE, JSON, REFERENCE,
MULTI_SELECT, SINGLE_SELECT, FILE_REFERENCE, COLOR, COUNTRY, LANGUAGE, BRAND, CERTIFICATION`.

Reference-typed values point at another registry by identifier: `COUNTRY`→`taxonomy.country.code`,
`BRAND`→`taxonomy.brand.id/code`, `CERTIFICATION`→`taxonomy.certification.id/code`,
`LANGUAGE`→`reference.supported_locale.code`, `ENUMERATION`/`SINGLE_SELECT`/`MULTI_SELECT`→
`taxonomy.enumeration_value.code` of the attribute's bound enumeration. `MEASUREMENT` carries a
numeric magnitude in the attribute's `unit_code`; `RANGE` carries min/max in that unit.

### `taxonomy.attribute`
| column | type | notes |
|---|---|---|
| id | uuid PK | |
| code | varchar(64) NOT NULL UNIQUE | stable id, e.g. `EYE_SIZE`, `LENS_INDEX` |
| data_type | varchar(32) NOT NULL | CHECK ∈ the 18 primitives |
| unit_code | varchar(32) NULL → taxonomy.unit_of_measure(code) | for MEASUREMENT/RANGE |
| enumeration_id | uuid NULL → taxonomy.enumeration(id) | for ENUMERATION/SINGLE_SELECT/MULTI_SELECT |
| min_value | numeric NULL | INTEGER/DECIMAL/MEASUREMENT/RANGE lower bound |
| max_value | numeric NULL | upper bound |
| min_length | int NULL | STRING lower length |
| max_length | int NULL | STRING upper length |
| regex_pattern | varchar(500) NULL | STRING pattern (RE2-safe; validated at save) |
| searchable | boolean NOT NULL DEFAULT false | search-index prep (ADR-025) |
| filterable | boolean NOT NULL DEFAULT false | |
| sortable | boolean NOT NULL DEFAULT false | |
| comparable | boolean NOT NULL DEFAULT false | product-comparison prep |
| facetable | boolean NOT NULL DEFAULT false | facet prep |
| seo | boolean NOT NULL DEFAULT false | eligible for SEO surfaces |
| visible | boolean NOT NULL DEFAULT true | hidden = internal-only |
| deprecated | boolean NOT NULL DEFAULT false | retained, not offered for new use |
| sort_order | int NOT NULL DEFAULT 0 | |
| ai_metadata | jsonb NULL | future AI hints (embeddings config, synonyms); opaque to the engine |
| source_version | int NOT NULL DEFAULT 1 | |
| + audit tail | | |

CHECK `ck_attribute_data_type`. Cross-field integrity (unit only with MEASUREMENT/RANGE; enumeration_id
only with ENUMERATION/SINGLE/MULTI) is validated in the service on write (kept out of SQL for clarity).

### `taxonomy.attribute_translation`
Translatable: `name varchar(300)`, `description varchar(4000)`. Full lifecycle shape.

### `taxonomy.attribute_allowed_value` (+ `_translation`)
Inline controlled values for attributes not backed by a shared enumeration (rare; prefer enumerations).
`attribute_allowed_value(id, attribute_id FK, value_code varchar(64), sort_order, active, +audit,
uq(attribute_id, value_code))`; translation carries `label varchar(300)`.

### `taxonomy.ai_metadata` note
`ai_metadata` and template `conditional` are the only `jsonb` columns; mapped in JPA via
`@JdbcTypeCode(SqlTypes.JSON)` on a `String`. Precedent: `audit.audit_log.detail` (V3). The
validation engine never introspects `ai_metadata`.

---

## 5. Measurement / unit registry

Units are de-duplicated (one row per symbol) with conversion metadata to a family base unit.

### `taxonomy.unit_of_measure`
| code | varchar(32) PK | symbol/stable id: `mm, cm, m, inch, diopter, micron, degree, gram, kg, piece, pair, carton, box, set` |
| family | varchar(32) NOT NULL | `LENGTH, MASS, POWER_DIOPTER, ANGLE, COUNT` (CHECK) |
| base_unit_code | varchar(32) NULL → self | family base (e.g. `mm`, `gram`); base rows self-reference / NULL |
| factor_to_base | numeric NULL | multiply to convert to base (e.g. `inch` → 25.4 `mm`) |
| offset_to_base | numeric NOT NULL DEFAULT 0 | affine offset (reserved; 0 for all seeded units) |
| display_format | varchar(64) NULL | e.g. `"{value} mm"`, `"{value} D"` |
| active | boolean | |
| sort_order | int | |
| + audit tail | | |
`ck_unit_family CHECK(...)`. **No duplicate units** (code PK). `COUNT` units (piece/pair/carton/box/set)
have no conversion (`factor_to_base` NULL) — they are not interconvertible.

### `taxonomy.unit_translation`
Translatable: `display_name varchar(120)` (e.g. "millimetre", "diopter"). Lifecycle shape (keyed by
`unit_code`).

---

## 6. Country registry (ISO 3166-1)

### `taxonomy.country`
| code | varchar(2) PK | ISO 3166-1 **alpha-2**, stable id |
| alpha3 | varchar(3) NOT NULL UNIQUE | ISO alpha-3 |
| numeric_code | varchar(3) NOT NULL | ISO numeric |
| region | varchar(64) NULL | UN region |
| subregion | varchar(64) NULL | |
| continent | varchar(32) NULL | |
| active | boolean NOT NULL DEFAULT true | |
| sort_order | int NOT NULL DEFAULT 0 | |
| + audit tail | | |
Seeded with the full ISO 3166-1 set. Future customs/logistics metadata attaches here without redesign.

### `taxonomy.country_translation`
Translatable: `name varchar(160)`. Lifecycle shape (keyed by `country_code`). Seeded `en` from ISO
short names.

---

## 7. Certification registry

### `taxonomy.certification`
| id | uuid PK | |
| code | varchar(64) NOT NULL UNIQUE | `ISO_9001, ISO_13485, ISO_14001, CE, FDA, ROHS, REACH, MEDICAL_DEVICE` |
| category | varchar(32) NULL | `QUALITY, SAFETY, ENVIRONMENTAL, MEDICAL, REGULATORY` (CHECK when present) |
| country_scope | varchar(2) NULL → taxonomy.country(code) | NULL = global |
| industry_scope | varchar(64) NULL | free code, e.g. `OPTICAL`, `MEDICAL_DEVICE` |
| validity_months | int NULL | typical validity window |
| deprecated | boolean NOT NULL DEFAULT false | |
| active | boolean NOT NULL DEFAULT true | |
| sort_order | int NOT NULL DEFAULT 0 | |
| + audit tail | | |

### `taxonomy.certification_translation`
Translatable: `name varchar(300)`, `description varchar(4000)`. Lifecycle shape.

---

## 8. Brand registry

Global brand registry; future supplier/product claims reference brands **by id/code**, never free-text
when a registry entry exists.

### `taxonomy.brand`
| id | uuid PK | |
| code | varchar(64) NOT NULL UNIQUE | stable id, e.g. `ESSILOR`, `RAY_BAN` |
| brand_type | varchar(32) NOT NULL | `LENS, FRAME, SUNGLASSES, CONTACT_LENS, MACHINE, ACCESSORY, GENERAL` (CHECK) |
| canonical_name | varchar(200) NOT NULL | language-neutral Latin canonical (proper noun) |
| owner_company | varchar(300) NULL | |
| manufacturer | varchar(300) NULL | |
| country_code | varchar(2) NULL → taxonomy.country(code) | origin |
| website | varchar(300) NULL | |
| logo_storage_key | varchar(300) NULL | object-storage key (ADR-023), never bytes in PG |
| approval_status | varchar(32) NOT NULL DEFAULT 'PENDING' | `PENDING, APPROVED, REJECTED, DEPRECATED` (CHECK) |
| active | boolean NOT NULL DEFAULT true | |
| source_version | int NOT NULL DEFAULT 1 | |
| + audit tail | | |

### `taxonomy.brand_translation`
Translatable: `display_name varchar(200)` (localized/script rendering, e.g. `依视路` for `ESSILOR`),
`description varchar(2000)`. Lifecycle shape. `canonical_name` stays language-neutral on the parent.

### `taxonomy.brand_alias`
| id uuid PK | brand_id uuid NOT NULL FK | alias varchar(200) NOT NULL | + audit |
`uq_brand_alias UNIQUE(brand_id, alias)` — synonym/legacy-name resolution for future matching.

---

## 9. Specification templates & validation engine

### `taxonomy.specification_template`
A category's specification layout. Effective template for a category = **its own template attributes
plus those inherited from ancestor categories** (resolved in the service; never duplicated in DB).
| id uuid PK | category_id uuid NOT NULL UNIQUE → taxonomy.category(id) | code varchar(64) NOT NULL UNIQUE | version int NOT NULL DEFAULT 1 | active boolean | + audit |
One template per category (`uq_specification_template_category UNIQUE(category_id)`); `version` bumps on
structural change.

### `taxonomy.specification_template_attribute`
| id uuid PK | template_id uuid NOT NULL FK | attribute_id uuid NOT NULL → taxonomy.attribute(id) | required boolean NOT NULL DEFAULT false | conditional jsonb NULL | default_value varchar(500) NULL | sort_order int NOT NULL DEFAULT 0 | + audit |
`uq_template_attribute UNIQUE(template_id, attribute_id)`, `ix_template_attribute_template (template_id)`.
`conditional` jsonb rule shape (engine-interpreted): `{"requiredWhen": {"attribute": "<CODE>", "equals":
"<VALUE_CODE>"}}` (Phase 8 supports `requiredWhen` equals/in; extensible).

### Validation engine (`SpecificationValidator`, taxonomy `api`)
Pure function: given a `categoryCode` and a `Map<attributeCode, rawValue>`, resolve the effective
template, then for each attribute validate:
- **required** (from template-attribute, incl. `conditional.requiredWhen`),
- **data-type parse** (INTEGER/DECIMAL/DATE/BOOLEAN/COLOR/MEASUREMENT/RANGE format),
- **numeric range** (`min_value`/`max_value`), **string length** (`min_length`/`max_length`), **regex**,
- **membership** for ENUMERATION/SINGLE_SELECT/MULTI_SELECT (value ∈ enumeration active values),
- **reference existence** for COUNTRY/BRAND/CERTIFICATION/LANGUAGE (id/code resolves & active),
- **unknown attribute** (not in effective template) → violation.

Returns `List<SpecificationViolation{ attributeCode, code, message }>` where `code` maps to a frontend
i18n key (problem+json `errors[]` convention). No product consumes it in Phase 8 — the engine ships with
exhaustive unit tests and a `POST /taxonomy/specifications/validate` demonstration endpoint. **Products
becoming part of the taxonomy is a STOP condition** — the engine validates *values against templates*, it
does not store product values.

---

## 10. Search / comparison foundation (preparation only)

Attributes carry `searchable/filterable/sortable/comparable/facetable/seo` flags. Phase 8 **only records
capability metadata** — it does not implement search, indexing, facets, or comparison. A read endpoint
exposes the flags so a future Search module can build its index config from the registry (single source
of truth). Implementing search is a STOP condition.

---

## 11. Administration & authorization

Taxonomy administration is **platform-staff-only**, authorized by platform capability (never an org
role), reusing the `platform_access` module. New `PlatformCapability` values (role→capability mapping is
Java, no migration):
- `TAXONOMY_READ` — read admin views (roles: `PLATFORM_ADMIN`, `SENIOR_REVIEWER`).
- `TAXONOMY_WRITE` — create/update/activate/deprecate/translate (role: `PLATFORM_ADMIN`).
- `BRAND_APPROVE` — approve/reject brands (role: `PLATFORM_ADMIN`).

Public read endpoints (`/taxonomy/**`, no auth) expose active, displayable registry data for future
modules and SEO. Admin endpoints (`/platform/taxonomy/**`) require the capability; non-staff receive 403,
and unknown ids are 404-concealed per platform policy. Every create/update/activate/deprecate/approve and
every translation upsert/approve emits an `audit` event (`taxonomy.<entity>.<action>`).

---

## 12. Seed data (V5 inline, codes + `en` `APPROVED` translations)

Seeded so future modules and tests have a coherent starting registry; other locales start `MISSING` and
are filled via admin/import (architecture supports all 20). Full contents generated in the migration:
- **Units:** the 14 listed units + families + conversions (length: mm base, cm×10, m×1000, inch×25.4,
  micron×0.001; mass: gram base, kg×1000; power: diopter; angle: degree; count: piece/pair/carton/box/set).
- **Countries:** full ISO 3166-1 (alpha-2/3/numeric/region/continent + `en` names).
- **Certifications:** ISO_9001, ISO_13485, ISO_14001, CE, FDA, ROHS, REACH, MEDICAL_DEVICE.
- **Enumerations + values:** FRAME_SHAPE, FRAME_MATERIAL, LENS_MATERIAL, LENS_DESIGN, LENS_COATING,
  LENS_INDEX, PHOTOCHROMIC_COLOR, POLARIZATION, GENDER, AGE_GROUP, MACHINE_VOLTAGE, POWER_SUPPLY,
  PACKAGING_TYPE (representative values each).
- **Category tree:** `OPTICAL`→(`LENS`→`PRESCRIPTION_LENS`→{`SINGLE_VISION`,`PROGRESSIVE`,`PHOTOCHROMIC`,
  `BLUE_FILTER`}), `FRAME`→{`METAL`→`TITANIUM`, `ACETATE`, `TR90`}, `SUNGLASSES`, `CONTACT_LENS`;
  `MACHINERY`→{`EDGER`,`TRACER`,`BLOCKER`,`DRILLER`,`INSPECTION`}, `LAB_EQUIPMENT`, `ACCESSORY`,
  `PACKAGING`. Codes + `en` names + primary `en` slugs.
- **Attributes + templates:** frame set (`EYE_SIZE`,`BRIDGE_WIDTH`,`TEMPLE_LENGTH`,`FRAME_WIDTH`,
  `LENS_HEIGHT`,`FRAME_MATERIAL`,`FRAME_SHAPE`,`GENDER`,`FRAME_COLOR`), lens set (`LENS_MATERIAL`,
  `LENS_INDEX`,`LENS_DESIGN`,`LENS_COATING`,`LENS_DIAMETER`,`BASE_CURVE`), machinery set
  (`VOLTAGE`,`POWER_WATT`,`NET_WEIGHT`). FRAME + LENS + EDGER specification templates wired to these.

Seeds are codes-first with an `en` `is_original=true APPROVED` translation row per entity. Reference-code
seed style follows V4 (inline `INSERT` after each `CREATE TABLE`).

---

## 13. Explicit non-goals (STOP conditions)

Product catalog, marketplace search, RFQ, quotations, buyer dashboard, messaging, orders, payments,
inventory, logistics, ERP, analytics, recommendations, AI matching. No product entity, no product values,
no search index. If products enter the taxonomy, localized names become identifiers, or a future optical
category would require schema redesign — **stop**.
