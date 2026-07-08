# E — User Roles & Permission Matrix

**Project:** SpexCrafters · **Date:** 2026-07-08

## E.1 Model

Authorization combines three layers, all enforced by the backend:

1. **Platform roles** — global, assigned by admins (`SUPER_ADMIN`, `PLATFORM_ADMIN`, `PLATFORM_MODERATOR`, `PLATFORM_SUPPORT`, `PLATFORM_AUDITOR`).
2. **Organization membership roles** — scoped to one organization (`ORG_OWNER`, `ORG_ADMIN`, `ORG_MEMBER`). A user may hold memberships in multiple organizations; every authenticated request in a portal context carries an active-organization scope.
3. **Capability roles** — org-scoped functional grants layered on membership: `BUYER_PROCUREMENT`, `SUPPLIER_SALES`, `SUPPLIER_CATALOG`. An organization has a type (`BUYER`, `SUPPLIER`, or `HYBRID`) constraining which capability roles are assignable.

Checks are **permission-based** in code (`rfq:create`, `product:publish`…), with roles as permission bundles; resource-ownership validation (org owns the RFQ/product/quotation) applies on every mutating call. Deny by default.

## E.2 Roles

| Role | Scope | Intended holder |
|---|---|---|
| Visitor | none | Unauthenticated traffic |
| Authenticated User | user | Registered, not yet in an org |
| ORG_OWNER | org | Founder/legal owner; billing; can transfer ownership; exactly one per org |
| ORG_ADMIN | org | Manages team, org profile, verification submissions |
| ORG_MEMBER | org | Baseline read access within org |
| BUYER_PROCUREMENT | org (buyer) | Creates/manages RFQs, awards quotations, messages suppliers |
| SUPPLIER_CATALOG | org (supplier) | Creates/edits/publishes products, media, spec sheets |
| SUPPLIER_SALES | org (supplier) | Views RFQs, submits/revises/withdraws quotations, messages buyers |
| PLATFORM_MODERATOR | platform | Product/RFQ/message moderation, abuse reports |
| PLATFORM_SUPPORT | platform | Read user/org context, impersonation-free assistance, unlock accounts |
| PLATFORM_ADMIN | platform | Verification decisions, taxonomy, CMS, config, templates |
| PLATFORM_AUDITOR | platform | Read-only: audit logs, security events, all admin views |
| SUPER_ADMIN | platform | Role grants, feature flags, destructive ops; MFA mandatory |

## E.3 Permission matrix

Legend: ✅ allowed · 🔶 allowed with constraint (noted) · — denied. "Org rows" assume the active organization owns the resource.

| Permission | Visitor | Auth User | ORG_MEMBER | ORG_ADMIN | ORG_OWNER | BUYER_PROC | SUP_CATALOG | SUP_SALES | MODERATOR | SUPPORT | ADMIN | AUDITOR | SUPER |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Browse public catalog/suppliers/content | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| View price tiers | 🔶 policy OQ-7 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Register / verify email | ✅ | — | — | — | — | — | — | — | — | — | — | — | — |
| Create organization | — | ✅ | — | — | — | — | — | — | — | — | ✅ | — | ✅ |
| Edit org profile | — | — | — | ✅ | ✅ | — | — | — | — | — | 🔶 on request | — | ✅ |
| Invite/remove members, assign org roles | — | — | — | ✅ | ✅ | — | — | — | — | — | — | — | ✅ |
| Transfer ownership / delete org | — | — | — | — | ✅ | — | — | — | — | — | — | — | ✅ |
| Submit verification documents | — | — | — | ✅ | ✅ | — | — | — | — | — | — | — | — |
| Favorites / saved searches | — | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — | — | — | — | — |
| Create/edit draft RFQ | — | — | — | 🔶 | 🔶 | ✅ | — | — | — | — | — | — | — |
| Publish RFQ (public or invited) | — | — | — | — | — | 🔶 verified org if flagged "verified-only" | — | — | — | — | — | — | — |
| Award / close RFQ | — | — | — | — | 🔶 | ✅ | — | — | — | — | — | — | — |
| View public RFQ board | — | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| View full RFQ detail (specs/files) | — | — | — | — | — | ✅ own | — | 🔶 supplier org meets RFQ qualification | ✅ | ✅ | ✅ | ✅ | ✅ |
| Submit / revise / withdraw quotation | — | — | — | — | — | — | — | ✅ | — | — | — | — | — |
| Compare quotations, shortlist | — | — | — | — | — | ✅ | — | — | — | — | — | — | — |
| Create/edit product (draft) | — | — | — | — | — | — | ✅ | — | — | — | — | — | — |
| Publish product | — | — | — | — | — | — | 🔶 org verification tier permitting | — | — | — | — | — | — |
| Message counterparty | — | — | — | — | — | ✅ | — | ✅ | — | — | — | — | — |
| Block / report abuse | — | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — | — | — | — | — |
| Moderate products/RFQs/messages | — | — | — | — | — | — | — | — | ✅ | — | ✅ | — | ✅ |
| Handle abuse reports / suspend org or user | — | — | — | — | — | — | — | — | ✅ suspend 🔶 (temp) | — | ✅ | — | ✅ |
| Approve/reject verification | — | — | — | — | — | — | — | — | — | — | ✅ | — | ✅ |
| Manage taxonomy (categories/attributes) | — | — | — | — | — | — | — | — | — | — | ✅ | — | ✅ |
| Manage CMS content / homepage / SEO / redirects | — | — | — | — | — | — | — | — | — | — | ✅ | — | ✅ |
| Manage currencies / exchange rates / templates | — | — | — | — | — | — | — | — | — | — | ✅ | — | ✅ |
| View audit logs / security events | — | — | — | — | — | — | — | — | — | — | ✅ | ✅ | ✅ |
| Manage feature flags / platform config | — | — | — | — | — | — | — | — | — | — | — | — | ✅ |
| Grant/revoke platform roles | — | — | — | — | — | — | — | — | — | — | — | — | ✅ |

Constraint notes: ORG_ADMIN/OWNER receive `BUYER_PROCUREMENT`-equivalent rights by default in buyer orgs (configurable); "award" may require ORG_OWNER co-approval above a target-price threshold (org policy setting). SUPPORT never mutates business data — support actions are limited to account unlock, email re-send, session revocation, all audit-logged.

## E.4 Enforcement rules

1. Every endpoint declares required permission(s) in code (annotation-based, e.g. `@RequiresPermission("rfq:award")`) — verified by ArchUnit tests that no controller is unannotated.
2. Org-scoped calls resolve the active org from the session context and verify membership + capability + resource ownership in the service layer.
3. Sensitive admin actions (verification decisions, suspensions, taxonomy changes, template edits, flag changes) write structured audit-log entries (actor, action, target, before/after, IP, correlation ID).
4. SUPER_ADMIN and PLATFORM_ADMIN require MFA; admin portal sessions are shorter-lived and IP-logged.
5. Frontend route protection mirrors the matrix for UX but is never the enforcement point.
