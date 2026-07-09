# ADR-023 — Object Storage & Evidence Upload

**Status:** Accepted — 2026-07-09 · **Detailed record:** [evidence-storage-architecture.md](../security/evidence-storage-architecture.md)

## Context
Supplier verification evidence (registration docs, certificates, factory photos) is security-sensitive binary content. It must never enter PostgreSQL, never be public, never be cross-tenant accessible, and must scale.

## Decision
- **`ObjectStorage` port** in a `media` module; business code never depends on the AWS SDK. **MinIO** for local/CI (non-prod creds, isolated private bucket), **AWS S3** for production. No production credentials locally or in CI.
- **Presigned staged upload:** `initiate` (server authorizes, creates metadata row + safe UUIDv7 key, returns short-lived presigned PUT) → client PUTs to storage → `finalize` (server verifies object presence, size, sha256, magic-byte vs declared type; idempotent). Chosen over pure backend-mediated (doesn't scale for large files) and pure client presign (no server finalization/validation).
- **Malware scanning deferred:** a `MalwareScanner` port with a no-op adapter that leaves evidence in `PENDING_SCAN` — the platform **never** marks evidence `CLEAN`. Fail-closed; quarantined/rejected never downloadable. We do not fake scanning.
- **Download** always re-authorized server-side (backend-streamed or short-lived presigned GET minted post-check); 404-concealment for non-owners; IDOR/cross-tenant tested.

## Alternatives
Store bytes in PostgreSQL (rejected — bloat, no streaming, backup cost); public bucket + obfuscated URLs (rejected — STOP condition: no public evidence); backend-proxy every byte (rejected as the sole path — poor scaling for large media, kept as the download option).

## Risks / migration
Deferred real scanning is an explicit interim limitation (documented, fail-closed). Swapping MinIO→S3 is a config + adapter concern; integrating a real scanner is a `MalwareScanner` implementation + a lifecycle transition, no schema change.
