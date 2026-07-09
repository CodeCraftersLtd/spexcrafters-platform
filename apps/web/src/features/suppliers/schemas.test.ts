import { describe, expect, it } from 'vitest';

import {
  companyInfoSchema,
  createApplicationSchema,
  facilitySchema,
} from './schemas';

// A translate function that echoes the key, so assertions can match on it.
const t = (key: string) => key;

describe('createApplicationSchema', () => {
  const schema = createApplicationSchema(t);

  it('accepts an original locale and legal name', () => {
    const result = schema.safeParse({ originalLocale: 'en', legalName: 'Acme Optics Ltd' });
    expect(result.success).toBe(true);
  });

  it('requires a non-empty legal name', () => {
    const result = schema.safeParse({ originalLocale: 'en', legalName: '  ' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.message).toBe('legalNameRequired');
    }
  });

  it('rejects an over-long legal name', () => {
    const result = schema.safeParse({ originalLocale: 'en', legalName: 'x'.repeat(301) });
    expect(result.success).toBe(false);
  });
});

describe('companyInfoSchema', () => {
  const schema = companyInfoSchema(t);

  it('accepts a minimal valid draft', () => {
    const result = schema.safeParse({
      legalName: 'Acme Optics Ltd',
      countryOfRegistration: 'cn',
      types: ['LENS_MANUFACTURER'],
      capabilities: ['LENS_COATING'],
    });
    expect(result.success).toBe(true);
    if (result.success) {
      // Country is upper-cased to satisfy the contract.
      expect(result.data.countryOfRegistration).toBe('CN');
    }
  });

  it('rejects an invalid country code', () => {
    const result = schema.safeParse({
      legalName: 'Acme',
      countryOfRegistration: 'CHINA',
      types: [],
      capabilities: [],
    });
    expect(result.success).toBe(false);
  });

  it('rejects an unknown supplier type code', () => {
    const result = schema.safeParse({
      legalName: 'Acme',
      types: ['NOT_A_TYPE'],
      capabilities: [],
    });
    expect(result.success).toBe(false);
  });

  it('rejects a malformed year', () => {
    const result = schema.safeParse({
      legalName: 'Acme',
      yearEstablished: '99',
      types: [],
      capabilities: [],
    });
    expect(result.success).toBe(false);
  });
});

describe('facilitySchema', () => {
  const schema = facilitySchema(t);

  it('accepts a valid facility', () => {
    const result = schema.safeParse({
      facilityTypeCode: 'FACTORY',
      country: 'vn',
      addressPrivacy: 'PUBLIC_CITY',
      ownership: 'OWNED',
      isPublic: true,
    });
    expect(result.success).toBe(true);
  });

  it('requires a valid country', () => {
    const result = schema.safeParse({
      facilityTypeCode: 'FACTORY',
      country: '',
      addressPrivacy: 'PUBLIC_CITY',
      ownership: 'OWNED',
      isPublic: true,
    });
    expect(result.success).toBe(false);
  });
});
