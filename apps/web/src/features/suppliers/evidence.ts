/**
 * Client-side evidence validation (framework-agnostic; unit-tested). Mirrors
 * the backend accept rules so the browser rejects an oversized or wrong-type
 * file BEFORE requesting a presigned upload ticket — the server re-validates
 * on finalize (magic bytes, size, sha256), this is a UX pre-check only.
 */

/** Accepted media types (contract: InitiateUploadRequest.mediaType). */
export const ACCEPTED_MEDIA_TYPES = [
  'application/pdf',
  'image/jpeg',
  'image/png',
  'image/webp',
] as const;

export type AcceptedMediaType = (typeof ACCEPTED_MEDIA_TYPES)[number];

/** 15 MiB ceiling, matching the backend limit. */
export const MAX_EVIDENCE_BYTES = 15 * 1024 * 1024;

/** `accept` attribute for the file input. */
export const EVIDENCE_ACCEPT_ATTR = ACCEPTED_MEDIA_TYPES.join(',');

export type EvidenceValidationCode = 'type' | 'size' | 'empty';

export interface EvidenceValidationError {
  code: EvidenceValidationCode;
  /** Interpolation values for the localized message (size errors only). */
  values?: { maxMiB: number; actualMiB: string };
}

export function isAcceptedMediaType(mediaType: string): mediaType is AcceptedMediaType {
  return (ACCEPTED_MEDIA_TYPES as readonly string[]).includes(mediaType);
}

/**
 * Validate a picked file against the accept rules. Returns null when valid, or
 * a coded error the caller renders through the `evidence` namespace.
 */
export function validateEvidenceFile(file: {
  type: string;
  size: number;
}): EvidenceValidationError | null {
  if (!isAcceptedMediaType(file.type)) {
    return { code: 'type' };
  }
  if (file.size <= 0) {
    return { code: 'empty' };
  }
  if (file.size > MAX_EVIDENCE_BYTES) {
    return {
      code: 'size',
      values: {
        maxMiB: MAX_EVIDENCE_BYTES / (1024 * 1024),
        actualMiB: (file.size / (1024 * 1024)).toFixed(1),
      },
    };
  }
  return null;
}
