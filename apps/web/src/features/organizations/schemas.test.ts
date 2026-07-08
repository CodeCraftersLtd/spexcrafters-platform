import { describe, expect, it } from 'vitest';

import en from '../../../messages/en.json';

import { createOrganizationSchema, inviteMemberSchema } from './schemas';

const validation = en.organizations.validation;

describe('createOrganizationSchema', () => {
  const schema = createOrganizationSchema(validation);

  it('accepts a valid organization and trims the name', () => {
    const result = schema.parse({
      name: '  Acme Optics  ',
      type: 'BUYER',
      country: 'DE',
    });
    expect(result).toEqual({ name: 'Acme Optics', type: 'BUYER', country: 'DE' });
  });

  it('accepts a missing or empty country', () => {
    expect(schema.parse({ name: 'Acme', type: 'SUPPLIER' }).country).toBeUndefined();
    expect(schema.parse({ name: 'Acme', type: 'HYBRID', country: '' }).country).toBe('');
  });

  it('uppercases a lowercase country code', () => {
    expect(schema.parse({ name: 'Acme', type: 'BUYER', country: 'de' }).country).toBe(
      'DE',
    );
  });

  it('rejects an empty name with the required message', () => {
    const result = schema.safeParse({ name: '   ', type: 'BUYER' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.message).toBe(validation.nameRequired);
    }
  });

  it('rejects a one-character name with the too-short message', () => {
    const result = schema.safeParse({ name: 'A', type: 'BUYER' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.message).toBe(validation.nameTooShort);
    }
  });

  it('rejects a name longer than 120 characters', () => {
    const result = schema.safeParse({ name: 'x'.repeat(121), type: 'BUYER' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.message).toBe(validation.nameTooLong);
    }
  });

  it('rejects an unknown organization type with the dictionary message', () => {
    const result = schema.safeParse({ name: 'Acme', type: 'WHOLESALER' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.message).toBe(validation.typeRequired);
    }
  });

  it('rejects malformed country codes', () => {
    for (const country of ['DEU', 'D', 'D1', '12']) {
      const result = schema.safeParse({ name: 'Acme', type: 'BUYER', country });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0]?.message).toBe(validation.countryInvalid);
      }
    }
  });
});

describe('inviteMemberSchema', () => {
  const schema = inviteMemberSchema(validation);

  it('accepts a valid MEMBER invitation', () => {
    expect(schema.parse({ email: 'ada@example.com', role: 'MEMBER' })).toEqual({
      email: 'ada@example.com',
      role: 'MEMBER',
    });
  });

  it('rejects OWNER as an invitation role', () => {
    const result = schema.safeParse({ email: 'ada@example.com', role: 'OWNER' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.message).toBe(validation.roleRequired);
    }
  });

  it('rejects an invalid email with the dictionary message', () => {
    const result = schema.safeParse({ email: 'not-an-email', role: 'MEMBER' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.message).toBe(validation.emailInvalid);
    }
  });
});
