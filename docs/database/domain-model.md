# I — Database Domain Model

**Project:** SpexCrafters · **Date:** 2026-07-08 · **Store:** PostgreSQL (system of record) · **Migrations:** Flyway only

## I.1 Global conventions

- **PKs:** `uuid` with UUIDv7 generation (time-ordered, index-friendly) via application or `pg_uuidv7`-equivalent function; sequences only for human-facing reference numbers (`rfq_no`, `quote_no`).
- **Audit columns on every business table:** `created_at timestamptz`, `updated_at timestamptz`, `created_by uuid`, `updated_by uuid`, `version int` (optimistic locking on concurrently-edited aggregates: products, rfqs, quotations, org profiles, content).
- **Soft delete** (`deleted_at`) only where business/compliance requires retention: organizations, users (anonymization instead where GDPR erasure applies), products (archived state instead), messages (moderation retention), audit rows are append-only.
- **Money:** `amount_minor bigint` + `currency char(3)` — never floats. Ranges (Rx) as `numeric(5,2)` pairs.
- **Text search:** generated `tsvector` columns + GIN; `pg_trgm` GIN indexes for fuzzy name matching.
- **Naming:** snake_case, singular module prefix only where collision risk exists; FKs `<entity>_id`.
- Schemas per bounded context (`identity.*`, `catalog.*`, …) to make module ownership physical; cross-schema FKs allowed for integrity but **cross-module table access is forbidden in code** (ArchUnit).

## I.2 Entities by module

### identity
| Table | Key fields | Notes |
|---|---|---|
| `user_account` | id, email (uq, citext), password_hash (argon2id), status, email_verified_at, locale, last_login_at | |
| `mfa_enrolment` | user_id FK, type (TOTP), secret (encrypted), confirmed_at, recovery_codes_hash | |
| `login_attempt` | user_id?, email, ip, user_agent, outcome, at | partitioned by month; brute-force queries |
| `refresh_token` / `session` | user_id, token_hash, expires_at, revoked_at, device fingerprint | if not Redis-held |
| `email_verification_token`, `password_reset_token` | user_id, token_hash, expires_at, used_at | single-use |

### organizations
| Table | Key fields | Notes |
|---|---|---|
| `organization` | id, slug (uq), legal_name, display_name, type (BUYER/SUPPLIER/HYBRID), country, status (ACTIVE/SUSPENDED), verification_tier | |
| `membership` | org_id, user_id, org_role (OWNER/ADMIN/MEMBER), status | uq(org_id,user_id) |
| `capability_grant` | membership_id, capability (BUYER_PROCUREMENT/SUPPLIER_SALES/SUPPLIER_CATALOG) | |
| `invitation` | org_id, email, proposed roles, token_hash, expires_at, accepted_by | |
| `platform_role_grant` | user_id, role, granted_by, granted_at | SUPER/ADMIN/MODERATOR/SUPPORT/AUDITOR |

### verification
`verification_request` (org_id, tier requested, status DRAFT/SUBMITTED/IN_REVIEW/APPROVED/REJECTED, decided_by, decided_at, rejection_reason) · `verification_document` (request_id, media_asset_id, doc_type: business_license/tax_cert/iso_cert/ce_doc/factory_audit…, expires_at) · decisions append-only for audit.

### suppliers / buyers
`supplier_profile` (org_id uq, company_type: MANUFACTURER/TRADING/LAB/COATING_LAB/DISTRIBUTOR, founded_year, employee_range, production_capacity, export_markets text[], languages text[], oem/odm/private_label bool, moq_note, lead_time_note, cover_media_id, logo_media_id, about localized) · `supplier_certification` (profile_id, cert_type, number, issuer, valid_to, media_asset_id, verified bool) · `supplier_trade_show` (name, year, booth) · `buyer_profile` (org_id uq, business_type, annual_volume_range) · `address` (org_id, type, country, region, city, lines, postal).

### catalog (taxonomy & attribute registry — the extensibility core)
| Table | Key fields | Notes |
|---|---|---|
| `category` | id, parent_id, slug (uq per locale via `category_i18n`), path ltree, vertical (LENSES/FRAMES/MACHINERY/ACCESSORIES/PACKAGING), sort, status | ltree for subtree queries |
| `attribute_definition` | id, code (uq, e.g. `lens.refractive_index`), data_type (TEXT/NUMERIC/BOOLEAN/ENUM/NUMERIC_RANGE/DIMENSION), unit, enum_values jsonb, validation jsonb (min/max/regex), is_filterable, is_comparable, is_required_for_publish, is_localized | **registry drives forms, validation, facets, and spec tables** |
| `category_attribute` | category_id, attribute_id, sort, group_label | binds attributes to categories; new categories = data, not DDL |
| `brand` | id, slug, name, owner_org_id? | |

**Attribute value strategy (hybrid, per brief §4/§17):** cross-category operational fields are real columns on `product` (moq, lead_time_days, country_of_origin, …). Category-specific technical values live in `product.spec jsonb`, keyed by `attribute_definition.code`, **validated server-side against the registry** on write, indexed with a GIN (`jsonb_path_ops`) plus targeted expression indexes for hot filterable attributes (e.g. `((spec->>'lens.refractive_index')::numeric)`). This satisfies "JSONB only for genuinely flexible attributes": the flexible part *is* the per-category spec, and it is schema-validated, not free-form. Adding a category or attribute is an admin operation, not a migration.

### products
| Table | Key fields | Notes |
|---|---|---|
| `product` | id, supplier_org_id, category_id, brand_id?, slug (uq), name, summary, status (DRAFT/PENDING_REVIEW/PUBLISHED/ARCHIVED/REJECTED), spec jsonb, moq int, lead_time_days_min/max, sample_available bool, oem/odm/private_label bool, country_of_origin, hs_code?, search_tsv tsvector (generated), version | optimistic lock |
| `product_i18n` | product_id, locale, name, summary, description | localized copy |
| `product_variant` | product_id, sku, variant attributes jsonb (color, size, Rx point), status | |
| `price_tier` | product_id (or variant_id), min_qty, amount_minor, currency, visibility (PUBLIC/AUTHED/VERIFIED) | uq(product, min_qty) |
| `product_media` | product_id, media_asset_id, sort, kind (IMAGE/VIDEO/SPEC_SHEET) | |
| `product_certification` | product_id, cert_type, media_asset_id? | CE/FDA/ISO… |

### rfq
| Table | Key fields | Notes |
|---|---|---|
| `rfq` | id, rfq_no (seq, e.g. RFQ-2026-000123), buyer_org_id, title, category_id, spec jsonb (registry-validated), description, quantity, unit, target_price_minor?, target_currency, destination_country, incoterm?, required_by date, visibility (PUBLIC/INVITED), qualification jsonb (verified_only, company_types, countries), status (DRAFT/OPEN/EVALUATING/AWARDED/CLOSED/EXPIRED/SUSPENDED), expires_at, awarded_quotation_id?, version | |
| `rfq_attachment` | rfq_id, media_asset_id | private ACL |
| `rfq_invitation` | rfq_id, supplier_org_id, status (SENT/VIEWED/DECLINED/QUOTED) | uq pair |

### quotations
| Table | Key fields | Notes |
|---|---|---|
| `quotation` | id, quote_no, rfq_id, supplier_org_id, status (SUBMITTED/REVISED/SHORTLISTED/AWARDED/REJECTED/WITHDRAWN/EXPIRED), current_revision_id, version | uq(rfq, supplier) — one active quote per supplier per RFQ |
| `quotation_revision` | quotation_id, revision_no, unit_price_minor, currency, **fx_rate_to_rfq_currency numeric + rate_date** (historical snapshot), moq, lead_time_days, incoterm, payment_terms, validity_until, notes, created_by | append-only; comparisons read revisions |
| `quotation_document` | revision_id, media_asset_id | |

### messaging
`conversation` (id, buyer_org_id, supplier_org_id, context_type (RFQ/PRODUCT/GENERAL), context_id?, last_message_at, buyer_archived/supplier_archived) · `message` (conversation_id, sender_user_id, sender_org_id, body, sent_at, edited_at?, moderation_status) · `message_attachment` (message_id, media_asset_id) · `read_marker` (conversation_id, org_id, last_read_message_id) · `org_block` (blocker_org_id, blocked_org_id) · `abuse_report` lives in administration.

### notifications
`notification` (recipient_user_id, type, payload jsonb, read_at, created_at; partition by month) · `notification_template` (code, locale, channel EMAIL/IN_APP, subject, body, version) · `email_outbox` (to, template_code, payload, status, attempts, next_attempt_at) — transactional outbox pattern.

### favorites / search
`favorite` (user_id, target_type PRODUCT/SUPPLIER, target_id; uq triple) · `saved_search` (user_id, name, query jsonb, alert_frequency NONE/DAILY/WEEKLY, last_run_at) · `search_document` (entity_type, entity_id, locale, tsv, trigram_text, facets jsonb, updated_at) — **projection table owned by search module**, rebuilt from events; the only table other modules never write.

### media
`media_asset` (id, owner_org_id?, uploaded_by, storage_key, bucket, mime, bytes, sha256, visibility PUBLIC/PRIVATE, scan_status PENDING/CLEAN/INFECTED, width/height?, created_at) · `media_derivative` (asset_id, kind THUMB/CARD/HERO/WEBP/AVIF, storage_key, width, height) · `upload_ticket` (presign metadata, expires_at, consumed_at).

### content / seo / localization / currency
`page`, `article` (+`article_i18n`, type INDUSTRY/GUIDE/BRAND), `event` (name, venue, country, starts/ends, website), `homepage_section` (kind, sort, config jsonb, status), `featured_item` (kind, target_id, slot, starts/ends) · `metadata_override` (route_pattern, locale, title, description, og fields) · `redirect` (from_path uq, to_path, http_status 301/410, hits) · `locale` (code, enabled, fallback) · `translation_bundle` (namespace, locale, key, value, updated_by) · `currency` (code, exponent, enabled) · `exchange_rate` (base, quote, rate numeric(18,8), as_of date, source; uq(base,quote,as_of)) — historical, never overwritten.

### audit / administration
`audit_log` (id, actor_user_id, actor_org_id?, action, target_type, target_id, before jsonb, after jsonb, ip, correlation_id, at; **append-only, partitioned by month**) · `security_event` (kind, user_id?, ip, details, at) · `moderation_case` (target_type, target_id, reason, status, assignee, resolution) · `abuse_report` (reporter_org_id, target_type, target_id, reason, status) · `feature_flag` (key, enabled, rules jsonb) · `platform_config` (key, value jsonb, version) · `job_queue` (type, payload, status, run_at, attempts, locked_by, locked_at) · `event_outbox` (aggregate_type, aggregate_id, event_type, payload, occurred_at, published_at).

## I.3 Relationship overview (core marketplace spine)

```
user_account ──< membership >── organization ──1 supplier_profile / buyer_profile
organization(supplier) ──< product >── category ──< category_attribute >── attribute_definition
product ──< product_variant ──< price_tier          product ──< product_media >── media_asset
organization(buyer) ──< rfq ──< rfq_invitation >── organization(supplier)
rfq ──< quotation ──< quotation_revision            rfq.awarded_quotation_id ─→ quotation
(conversation) buyer_org ── supplier_org, context → rfq | product
```

## I.4 Index plan (initial, beyond PK/FK/uniques)

| Index | Purpose |
|---|---|
| `product(status, category_id)` btree; `product(supplier_org_id, status)` | listing queries |
| `product USING GIN (search_tsv)`; `search_document USING GIN (tsv)`; `… gin_trgm_ops` on names | FTS + typo tolerance |
| `product USING GIN (spec jsonb_path_ops)` + expression indexes per hot attribute | faceted spec filtering |
| `category USING GIST (path)` (ltree) | subtree category filters |
| `rfq(status, category_id, expires_at)` partial `WHERE status='OPEN'` | RFQ board |
| `quotation(rfq_id, status)`; `quotation(supplier_org_id, status)` | inboxes |
| `message(conversation_id, sent_at DESC)`; `conversation(buyer_org_id, last_message_at DESC)` (+supplier mirror) | thread pagination |
| `notification(recipient_user_id, read_at) partial WHERE read_at IS NULL` | unread counts |
| `price_tier(product_id, min_qty)` | tier resolution |
| `audit_log(target_type, target_id, at)`; BRIN on `at` for partitions | investigations |
| `login_attempt(email, at DESC)`, `(ip, at DESC)` | brute-force detection |
| `exchange_rate(base, quote, as_of DESC)` | latest-rate lookup |

Cursor pagination (keyset on `(sort_key, id)`) for all public feeds; offset pagination only in small admin tables.

## I.5 Migration & data strategy

1. **Flyway** versioned migrations per module directory (`db/migration/V{n}__{module}_{desc}.sql`), single linear history; repeatable migrations for views/functions. Hibernate `ddl-auto=validate` everywhere; never `update`.
2. **Expand → migrate → contract** for zero-downtime changes; destructive steps ship ≥1 release after their replacement.
3. CI gates: migration runs against Testcontainers PG from empty AND from latest-release schema snapshot; `EXPLAIN ANALYZE` checks on the top-20 query budget list.
4. **Seed strategy:** taxonomy + attribute registry for the three verticals (lenses per brief §17: material, index, Abbe, UV cutoff, designs, Rx ranges, coatings…; frames: materials, rim types, dimensions…; machinery: category, power, capacity…) ships as reference-data migrations; demo/dev fixtures live outside Flyway.
5. **No competitor data import.** SpexCrafters is an independent platform; OpticLeague is strictly a functional benchmark. No competitor data, datasets, or ERP content are imported. Future third-party ERP integrations (for SpexCrafters' own suppliers) would be separate, explicitly-approved workstreams — the schema above is integration-compatible by design.
6. Backups: automated daily base + WAL for PITR, encrypted, offsite copy; restore rehearsal quarterly (RPO ≤ 15 min, RTO ≤ 4 h targets — confirmed in DR doc).
