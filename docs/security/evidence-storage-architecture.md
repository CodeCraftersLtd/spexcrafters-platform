# Evidence Management & Object Storage Architecture (Phase 7)

**Status:** Binding (§§38–42). Evidence is security-sensitive. Bytes never enter PostgreSQL; only metadata does.

## 1. Object storage (ADR-023)
- **`ObjectStorage` port** (interface) in a `media` module; business modules never import the AWS SDK. Adapter: S3-compatible. **Local/CI: MinIO** (non-production credentials, isolated bucket `spexcrafters-evidence`, no public access). **Production target: AWS S3** (private bucket, SSE, no public ACL). No production credentials locally or in CI.
- Buckets are **private**; no public bucket, no public object URLs, no guessable keys.
- **Safe keys:** `evidence/{organizationId}/{supplierId}/{evidenceId}` where `evidenceId` is UUIDv7 — server-generated, not derived from filename. Original filename stored only as metadata. No path traversal (filename sanitized, never used in the key).

## 2. Upload architecture (ADR-023 decision)
**Presigned direct-to-storage upload with server authorization + server finalization** (staged):
1. `initiate` (authorized: `supplier.evidence.upload`, org owns supplier) → server creates an `evidence` row in state `PENDING_SCAN`/upload-pending, generates the safe key, returns a **short-lived presigned PUT** (size + content-type constrained) + the `evidenceId`. Audited `supplier.evidence.upload_initiated`.
2. Client PUTs bytes directly to storage.
3. `finalize` (authorized) → server verifies the object exists, checks **byte size** against the declared/limit, reads it to compute **sha256** and validate **magic bytes** vs declared media type (allowlist: pdf, jpeg, png, webp; images + PDF only in Phase 7), rejects mismatch/oversize/missing-object, sets metadata, moves scan_status to `PENDING_SCAN`. Audited `supplier.evidence.uploaded`/`finalized`. Finalize is idempotent per `evidenceId`.
- Overwrite prevention (key includes unique id); abandoned/incomplete uploads: rows not finalized within a TTL are reaped (documented job; Phase-7 marks them and a cleanup query — no orphan bytes claimed as evidence). No raw signed URLs in logs (masked).

## 3. Malware policy (ADR — deferred scanner) (§41)
- **Real malware scanning is NOT implemented in Phase 7 — stated explicitly.** Evidence is **never** marked `CLEAN` by the platform. A `MalwareScanner` port exists (future integration); the default no-op adapter leaves evidence in `PENDING_SCAN` (it never advances to `CLEAN`).
- **Fail-closed:** only evidence in an explicitly reviewer-downloadable state may be fetched. Since nothing reaches `CLEAN` in Phase 7, reviewer download is gated on `scan_status IN (PENDING_SCAN)` **only under an explicit, audited reviewer override** documented as an interim control — OR (chosen default) reviewers may download `PENDING_SCAN` evidence through a backend-streamed, audited path with a visible "not malware-scanned" banner. States: `PENDING_SCAN → SCANNING → CLEAN | REJECTED | QUARANTINED`. `QUARANTINED`/`REJECTED` are never downloadable. We do not fake CLEAN.

## 4. Download authorization (§42)
- Evidence fetch requires authorization every time: supplier-org users with `supplier.evidence.read` may fetch **only their own org's** evidence; platform staff with `supplier.review.read` may fetch any (audited); other suppliers and the public **cannot**. 404-concealment for non-owners (mirrors org tenancy).
- Delivery: **backend-authorized streaming** (default — no URL leaves the server) or short-lived presigned GET minted only after the authz check; either way the check is server-side and audited. Tested: IDOR, cross-tenant, unauthorized reviewer, expired access, deleted evidence, quarantined evidence.

## 5. Retention & deletion
- `retention_status` guards deletion of evidence tied to a decided verification (not silently destroyed). Supplier `supplier.evidence.delete` removes only unreferenced draft evidence; referenced evidence follows retention policy. Deletion is audited; audit history is never deleted.

## 6. Tests (§60)
Authorized initiate; unauthorized denied; cross-tenant denied; disallowed type; oversize; unsafe filename → safe key; finalize without object denied; checksum mismatch; cross-tenant read denied; public denied; unauthorized reviewer denied; expired access; deleted unavailable; quarantined unavailable; retention-protected not destroyed. Via Testcontainers + MinIO in CI (health-checked, isolated bucket, cleanup).
