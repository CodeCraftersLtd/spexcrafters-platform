-- V3: Phase 6 session hardening — structured audit detail (TD-9).
-- Conventions per docs/database/domain-model.md §I.1 (see V1).
--
-- audit.audit_log.detail carries the structured payload of an event as jsonb
-- (e.g. the checked capability of an authorization denial, the family id of a
-- refresh-token replay). Nullable: most events have no payload beyond the
-- target columns. No index — the column is investigative, not a query path.
--
-- The refresh-token grace window and absolute session lifetime (session-security-policy.md
-- §§1–2) need no new columns: the rotation instant is the successor row's created_at
-- (reachable via replaced_by) and the family age is min(created_at) over family_id,
-- which ix_refresh_token_family_id already serves.

ALTER TABLE audit.audit_log
    ADD COLUMN detail jsonb;
