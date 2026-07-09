import { describe, expect, it } from 'vitest';

import {
  ACCEPTED_MEDIA_TYPES,
  MAX_EVIDENCE_BYTES,
  isAcceptedMediaType,
  validateEvidenceFile,
} from './evidence';

describe('evidence client validation', () => {
  it('accepts the four contract media types', () => {
    for (const type of ACCEPTED_MEDIA_TYPES) {
      expect(isAcceptedMediaType(type)).toBe(true);
      expect(validateEvidenceFile({ type, size: 1024 })).toBeNull();
    }
  });

  it('rejects an unsupported media type', () => {
    const result = validateEvidenceFile({ type: 'image/gif', size: 1024 });
    expect(result).toEqual({ code: 'type' });
  });

  it('rejects an empty file', () => {
    expect(validateEvidenceFile({ type: 'application/pdf', size: 0 })).toEqual({
      code: 'empty',
    });
  });

  it('accepts a file at exactly the 15 MiB ceiling', () => {
    expect(
      validateEvidenceFile({ type: 'image/png', size: MAX_EVIDENCE_BYTES }),
    ).toBeNull();
  });

  it('rejects a file one byte over the ceiling and reports sizes', () => {
    const result = validateEvidenceFile({
      type: 'image/png',
      size: MAX_EVIDENCE_BYTES + 1,
    });
    expect(result?.code).toBe('size');
    expect(result?.values?.maxMiB).toBe(15);
    expect(result?.values?.actualMiB).toBe('15.0');
  });
});
