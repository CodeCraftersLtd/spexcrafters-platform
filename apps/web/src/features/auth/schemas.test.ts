import { describe, expect, it } from 'vitest';

import en from '../../../messages/en.json';

import {
  createLoginSchema,
  createRegisterSchema,
  createResendVerificationSchema,
} from './schemas';

const v = en.auth.validation;

describe('createRegisterSchema', () => {
  const schema = createRegisterSchema(v);

  it('accepts a valid registration', () => {
    const result = schema.safeParse({
      displayName: 'Ada Lovelace',
      email: 'ada@example.com',
      password: 'correct-horse-battery',
    });
    expect(result.success).toBe(true);
  });

  it('trims surrounding whitespace on displayName and email', () => {
    const result = schema.parse({
      displayName: '  Ada Lovelace  ',
      email: '  ada@example.com  ',
      password: 'correct-horse-battery',
    });
    expect(result.displayName).toBe('Ada Lovelace');
    expect(result.email).toBe('ada@example.com');
  });

  it('rejects an empty display name with the dictionary message', () => {
    const result = schema.safeParse({
      displayName: '   ',
      email: 'ada@example.com',
      password: 'correct-horse-battery',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.map((issue) => issue.message)).toContain(
        v.displayNameRequired,
      );
    }
  });

  it('rejects a display name over 120 characters', () => {
    const result = schema.safeParse({
      displayName: 'a'.repeat(121),
      email: 'ada@example.com',
      password: 'correct-horse-battery',
    });
    expect(result.success).toBe(false);
  });

  it('rejects an invalid email with the dictionary message', () => {
    const result = schema.safeParse({
      displayName: 'Ada',
      email: 'not-an-email',
      password: 'correct-horse-battery',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.map((issue) => issue.message)).toContain(
        v.emailInvalid,
      );
    }
  });

  it('rejects an email over 254 characters', () => {
    const local = 'a'.repeat(250);
    const result = schema.safeParse({
      displayName: 'Ada',
      email: `${local}@ex.com`,
      password: 'correct-horse-battery',
    });
    expect(result.success).toBe(false);
  });

  it('rejects passwords shorter than 12 characters', () => {
    const result = schema.safeParse({
      displayName: 'Ada',
      email: 'ada@example.com',
      password: 'short-pass',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.map((issue) => issue.message)).toContain(
        v.passwordTooShort,
      );
    }
  });

  it('accepts a 12-character password and rejects 129 characters', () => {
    const base = {
      displayName: 'Ada',
      email: 'ada@example.com',
    };
    expect(schema.safeParse({ ...base, password: 'x'.repeat(12) }).success).toBe(true);
    expect(schema.safeParse({ ...base, password: 'x'.repeat(129) }).success).toBe(false);
  });
});

describe('createLoginSchema', () => {
  const schema = createLoginSchema(v);

  it('accepts valid credentials', () => {
    expect(
      schema.safeParse({ email: 'ada@example.com', password: 'p' }).success,
    ).toBe(true);
  });

  it('requires a password with the dictionary message', () => {
    const result = schema.safeParse({ email: 'ada@example.com', password: '' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.map((issue) => issue.message)).toContain(
        v.passwordRequired,
      );
    }
  });

  it('requires a syntactically valid email', () => {
    expect(schema.safeParse({ email: 'nope', password: 'p' }).success).toBe(false);
  });
});

describe('createResendVerificationSchema', () => {
  const schema = createResendVerificationSchema(v);

  it('accepts a valid email and rejects an empty one', () => {
    expect(schema.safeParse({ email: 'ada@example.com' }).success).toBe(true);
    const empty = schema.safeParse({ email: '' });
    expect(empty.success).toBe(false);
    if (!empty.success) {
      expect(empty.error.issues.map((issue) => issue.message)).toContain(
        v.emailRequired,
      );
    }
  });
});
