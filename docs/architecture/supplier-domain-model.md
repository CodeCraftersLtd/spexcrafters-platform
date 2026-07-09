# Supplier Onboarding & Verification — Domain Model (Phase 7)

**Status:** Binding. Bounded contexts: `supplier`, `verification`, `platform-access` (media/evidence in [evidence-storage-architecture.md](../security/evidence-storage-architecture.md)). Builds on the Organizations capability model; **no parallel tenancy**. Minimum-coherent scope per the brief — entities below are the ones we build; the brief's longer lists are a menu.

## 1. Ownership & tenancy
- Every supplier belongs to **exactly one organization**; an organization has **at most one active supplier identity** (partial unique index on `organization_id WHERE status <> 'WITHDRAWN'`... see §7). Supplier authorization = org capabilities (§5) layered on the existing membership model. Users act through organizations; no supplier-local accounts.

## 2. Onboarding vs Verification (separate!)
- **Onboarding** = who the company is / what it claims (application + profile + declared types/capabilities + evidence).
- **Verification** = which claims SpexCrafters reviewed, with what evidence, by whom, when, with what validity. **No `verified` boolean anywhere** — scope-based only.

## 3. Entities (minimum set)
| Entity | Key fields |
|---|---|
| `Supplier` | id, organization_id (uq active), operational_status {PENDING, ACTIVE, SUSPENDED, DEACTIVATED}, original_locale, created/updated audit |
| `SupplierApplication` | id, supplier_id, status (lifecycle §4), submitted_at, decided_at, decided_by, `version` (optimistic) |
| `SupplierProfile` | id, supplier_id, legal_name (E), registered_legal_name_translated? (E, optional official), trading_name (D), registration_number (E), country_of_registration (E, ISO), registration_authority, registration_date, company_type_code (C), year_established, employee_range, website, business_email, business_phone, export_markets (C codes), languages (locale codes), source_version |
| `SupplierProfileTranslation` | id, profile_id, locale, trading_name, company_description, production_capability_description, oem_description, odm_description, private_label_description, quality_control_description, export_market_description + lifecycle cols (ADR-020); uq(profile_id, locale) |
| `SupplierTypeAssignment` | supplier_id, type_code (C); uq pair |
| `SupplierCapabilityDeclaration` | supplier_id, capability_code (C), claim_status {CLAIMED, EVIDENCE_SUBMITTED, VERIFIED, REJECTED}; uq pair |
| `SupplierFacility` | id, supplier_id, facility_type_code (C), country (ISO), region, city, address_privacy {PUBLIC_CITY, PRIVATE}, ownership {OWNED, LEASED, PARTNER}, public bool, source_version |
| `SupplierFacilityTranslation` | id, facility_id, locale, name, description + lifecycle; uq(facility_id, locale) |
| `VerificationCase` | id, supplier_id, status {NOT_REQUESTED, PENDING, UNDER_REVIEW, VERIFIED, REJECTED, CHANGES_REQUESTED, EXPIRED, SUSPENDED, REVOKED}, opened_at |
| `VerificationScopeResult` | id, case_id, scope_code (C), status (same enum subset), decided_by, decided_at, valid_from, valid_until?, reason, `evidence refs`; uq(case_id, scope_code) |
| `VerificationEvidence` | id, supplier_id, organization_id, evidence_type_code (C), storage_key, original_filename, media_type, byte_size, sha256, uploaded_by, uploaded_at, scan_status {PENDING_SCAN, SCANNING, CLEAN, REJECTED, QUARANTINED}, review_status {UNREVIEWED, ACCEPTED, REJECTED}, reviewed_by, reviewed_at, document_locale?, retention_status; (see storage doc) |
| `ReviewRequest` (change request) | id, application_id, requested_item, reason, requested_by, requested_at, status {OPEN, RESPONDED, RESOLVED}, supplier_response?, response_locale?, resolved_at |
| `VerificationDecision` (audit-facing) | recorded as audit events (§56) + the scope-result rows; no separate god-table |

Codes (C) come from seeded reference tables with stable codes; **labels live in UI resources** (`taxonomy` namespace).

## 4. Application lifecycle
`DRAFT → SUBMITTED → UNDER_REVIEW → (CHANGES_REQUESTED → RESUBMITTED → UNDER_REVIEW)* → APPROVED | REJECTED`; `WITHDRAWN` from any pre-decision state; `SUSPENDED` acts on the **Supplier**, not the application. **Approval activates the Supplier** (`Supplier.operational_status = ACTIVE`) — documented decision: APPROVED is a terminal application state *and* the trigger that flips supplier operational status in the same transaction. Public visibility remains a separate policy flag (not built beyond the profile foundation). Allowed transitions, required capability, actor, validation, audit event, and idempotency are enforced in the application service (mirroring the organizations state machine); invalid transitions → 409.

## 5. Supplier (org-scoped) capabilities
`supplier.create`, `supplier.read`, `supplier.update`, `supplier.submit`, `supplier.withdraw`, `supplier.evidence.read`, `supplier.evidence.upload`, `supplier.evidence.delete`, `supplier.verification.read`. Granted to org roles: OWNER/ADMIN get all; MEMBER gets read-only (`supplier.read`, `supplier.verification.read`). **Never** grants platform moderation.

## 6. Platform-staff authorization (separate from org roles)
New `platform-access` context: `PlatformStaff` (user_id, platform_role {REVIEWER, SENIOR_REVIEWER, PLATFORM_ADMIN}, active) + typed `PlatformCapability`: `supplier.review.read`, `supplier.review.claim`, `supplier.review.request_changes`, `supplier.review.approve`, `supplier.review.reject`, `supplier.suspend`, `supplier.verification.grant`, `supplier.verification.suspend`, `supplier.verification.revoke`. Role→capability matrix documented in this context. **Enforced in the backend application layer**; org OWNER can never obtain review capabilities. Staff provisioning: a seeded/`platform_staff`-table row inserted by a documented bootstrap fixture/migration (no email allowlist, no hidden-route "security"). Full admin IAM is out of scope.

## 7. Key invariants (tested)
- Second active supplier for an org → denied. Unauthorized org member → denied. Claim never becomes verified without a reviewer action. Verification grant requires a reviewer capability + evidence linkage. No generic verified boolean. Original-language content never overwritten by a translation. Translation goes stale when `source_version` bumps. Reviewer decisions and suspensions/revocations are audited (append-only). Evidence isolation per organization (storage doc).

## 8. Approval / rejection / suspension / revocation semantics
- Approval → Supplier ACTIVE; scopes granted **separately and explicitly** (never auto from claims).
- Rejection → application REJECTED; reapplication policy: new application allowed (documented), evidence retained per retention.
- Supplier suspension ≠ verification-scope suspension ≠ verification-scope revocation — three distinct actions, each audited; audit history never deleted.

## 9. Migrations
New Flyway `V4+` (never touch V1/V2/V3): supplier, application, profile(+translation), type/capability assignments, facility(+translation), verification case/scope-result, evidence metadata, review requests, platform_staff, reference tables (types/capabilities/scopes/evidence-types/facility-types) with stable codes. FKs, uniques, check constraints, partial unique (one active supplier / one translation per locale), reviewer-queue and ownership and locale indexes. Tested from zero and V1→V2→V3→V4 via Testcontainers.
