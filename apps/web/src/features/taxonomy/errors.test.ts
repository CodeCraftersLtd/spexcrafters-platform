import { describe, expect, it } from 'vitest';

import type { Translator } from '@/i18n/translator';

import { translateTaxonomyError } from './errors';

/** Minimal translator over a fixed message map. */
function translator(messages: Record<string, string>): Translator {
  const fn = ((key: string) => messages[key] ?? key) as Translator;
  fn.has = (key: string) => key in messages;
  return fn;
}

describe('translateTaxonomyError', () => {
  const t = translator({
    forbidden: 'You do not have permission to do that.',
    conflict: 'That action conflicts with the current state.',
    unexpected: 'Something went wrong.',
  });

  it('uses the localized copy for a known code', () => {
    expect(translateTaxonomyError({ code: 'conflict', message: 'raw' }, t)).toBe(
      'That action conflicts with the current state.',
    );
  });

  it('falls back to the server message for an unknown code', () => {
    expect(
      translateTaxonomyError({ code: 'duplicate-code', message: 'Code exists' }, t),
    ).toBe('Code exists');
  });

  it('falls back to the generic error when nothing else is available', () => {
    expect(translateTaxonomyError({ code: 'weird', message: '' }, t)).toBe(
      'Something went wrong.',
    );
  });
});
