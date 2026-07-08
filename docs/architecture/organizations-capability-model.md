# Organizations — Capability Model, Role Matrix & Lifecycle Semantics

**Status:** Binding for the Phase-5 vertical slice · 2026-07-09
**Scope:** organizations bounded context only. Supplier/buyer capability roles (`SUPPLIER_SALES` etc.) arrive with their own slices and layer on top of this model.

## 1. Typed capabilities

Authorization is decided against a closed, typed `Capability` enum — never scattered string comparisons. Backend enum values (wire names in parentheses appear in `callerCapabilities`):

| Capability | Wire name | Grants |
|---|---|---|
| `ORGANIZATION_READ` | `organization.read` | View the organization resource |
| `ORGANIZATION_UPDATE` | `organization.update` | Modify organization profile fields |
| `MEMBERS_READ` | `organization.members.read` | List members and invitations |
| `MEMBERS_INVITE` | `organization.members.invite` | Create and revoke invitations |
| `MEMBERS_REMOVE` | `organization.members.remove` | Remove members (rank rules below) |
| `ROLES_MANAGE` | `organization.roles.manage` | Change member roles (incl. OWNER grants) |

`organization.delete` is **not implemented** in this slice (no deletion capability exists; documented limitation — deletion policy arrives with compliance workstream).

## 2. Roles and the role→capability matrix

`OrganizationRole` = `OWNER` > `ADMIN` > `MEMBER` (strict rank order used by rank rules).

| Capability | OWNER | ADMIN | MEMBER |
|---|---|---|---|
| organization.read | ✅ | ✅ | ✅ |
| organization.update | ✅ | ✅ | — |
| organization.members.read | ✅ | ✅ | ✅ |
| organization.members.invite | ✅ | ✅ (MEMBER-role invitations only) | — |
| organization.members.remove | ✅ (any, invariants permitting) | ✅ (strictly-lower rank only, i.e. MEMBER) | — |
| organization.roles.manage | ✅ | — | — |

**Rank rules (privilege-escalation prevention):**
- An actor may never grant, invite to, or assign a role ≥ their own rank unless they are OWNER.
- ADMIN may invite/remove only `MEMBER`-role targets; may not touch ADMIN or OWNER memberships.
- Only OWNER holds `roles.manage`; role changes to/from `OWNER` are OWNER-only. Multiple OWNERs are permitted.
- Self role-change is forbidden (prevents accidental lockout and self-escalation by definition).

## 3. Membership lifecycle

`MembershipStatus` = `ACTIVE` | `REMOVED` (terminal; removed rows retained for audit). Re-joining creates a **new** membership row — enforced by a partial unique index on `(organization_id, user_id) WHERE status='ACTIVE'`.

- **Initial owner:** organization creation persists the organization and the creator's `OWNER` membership **in one transaction** (atomic).
- **Removal:** requires `members.remove` + rank rule; **self-removal (leave)** is always permitted **except** for the last OWNER.
- **Last-owner invariant:** the count of `ACTIVE` `OWNER` memberships may never reach 0 while the organization is `ACTIVE`. Enforced in the application service inside a transaction holding a `PESSIMISTIC_WRITE` lock on the organization row for every owner-affecting mutation (remove-owner, demote-owner, owner-leave) — this serializes concurrent demotions/removals. Violations → 409 problem `last-owner`.
- **Ownership transition:** promote another ACTIVE member to OWNER first (OWNER-only), then the original owner may leave or be demoted.
- **Inactive denial:** `REMOVED` membership confers no capabilities; requests behave as non-member.

## 4. Invitations

- Created with `email` (citext) + role ∈ {`ADMIN`,`MEMBER`} (OWNER cannot be invited; promote after joining).
- **Token:** 32-byte cryptographically random, URL-safe; **only the SHA-256 hash persists**; raw token appears exactly once in the invitation email; never logged.
- **Expiry** 7 days · **single-use** (`PENDING → ACCEPTED` sets `accepted_at/accepted_by`) · states: `PENDING | ACCEPTED | REVOKED | EXPIRED`.
- **Binding:** acceptance requires an authenticated user whose verified account email equals the invitation email (case-insensitive); mismatch → 403 problem `invitation-identity-mismatch` (no org details leaked). Replay of consumed/revoked/expired tokens → 410.
- One `PENDING` invitation per (organization, email) — partial unique index.
- If the invitee already has an `ACTIVE` membership → 409 `duplicate-membership` at accept time (and creation is rejected if detectable).

## 5. Tenancy & concealment

- Every organization-owned row carries `organization_id`; module-external references use typed IDs only.
- Non-members receive **404 (`not-found` problem)** for any `/organizations/{id}` resource — existence concealment; membership-holders lacking a capability receive **403** (they already know the org exists). `authorization.denied` audit events record org-scoped 403s and concealed 404s (actor, org, capability checked).
- Organization IDs from the client are never trusted: every org-scoped request resolves user → org → ACTIVE membership → role → capabilities → decision in the application service (`OrganizationAccess` policy), not in controllers.

## 6. Audit actions (module-owned)

`organization.created` · `organization.updated` · `organization.invitation.created` · `organization.invitation.revoked` · `organization.invitation.accepted` (≙ member.added) · `organization.member.removed` · `organization.member.role_changed` · `authorization.denied` — each with actor user ID, organization ID, target (membership/invitation ID), timestamp, correlation ID; never raw tokens, never credentials.
