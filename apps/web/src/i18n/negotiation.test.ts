import { describe, expect, it } from 'vitest';

import {
  matchAcceptLanguage,
  negotiateLocale,
  parseAcceptLanguage,
} from './negotiation';

describe('parseAcceptLanguage', () => {
  it('orders ranges by descending quality', () => {
    const ranges = parseAcceptLanguage('fr;q=0.5, de, en;q=0.8');
    expect(ranges.map((r) => r.tag)).toEqual(['de', 'en', 'fr']);
  });

  it('drops q=0 ranges', () => {
    const ranges = parseAcceptLanguage('en;q=0, de');
    expect(ranges.map((r) => r.tag)).toEqual(['de']);
  });
});

describe('matchAcceptLanguage', () => {
  it('returns the first supported match', () => {
    expect(matchAcceptLanguage('fr-CH, fr;q=0.9, en;q=0.8')).toBe('fr');
    expect(matchAcceptLanguage('zh-Hans, en')).toBe('zh-CN');
  });

  it('returns en for a wildcard', () => {
    expect(matchAcceptLanguage('*')).toBe('en');
  });

  it('returns null when nothing matches', () => {
    expect(matchAcceptLanguage('xx, yy')).toBeNull();
    expect(matchAcceptLanguage(null)).toBeNull();
    expect(matchAcceptLanguage('')).toBeNull();
  });
});

describe('negotiateLocale — ADR-019 priority', () => {
  it('1. an explicit URL locale wins over everything', () => {
    expect(
      negotiateLocale({
        pathLocale: 'ar',
        cookieLocale: 'de',
        acceptLanguage: 'fr',
      }),
    ).toBe('ar');
  });

  it('2. the sc_locale cookie wins over Accept-Language (explicit choice persists)', () => {
    expect(
      negotiateLocale({
        pathLocale: null,
        cookieLocale: 'de',
        acceptLanguage: 'fr,en;q=0.9',
      }),
    ).toBe('de');
  });

  it('3. Accept-Language is used on first visit (no path, no cookie)', () => {
    expect(
      negotiateLocale({ acceptLanguage: 'fr-FR, fr;q=0.9, en;q=0.5' }),
    ).toBe('fr');
  });

  it('4. falls back to en when nothing resolves', () => {
    expect(negotiateLocale({})).toBe('en');
    expect(negotiateLocale({ acceptLanguage: 'xx' })).toBe('en');
  });

  it('ignores an unsupported URL/cookie value and continues down the chain', () => {
    expect(
      negotiateLocale({
        pathLocale: 'zh-Hans', // alias, not a canonical code → not accepted here
        cookieLocale: 'de',
        acceptLanguage: 'fr',
      }),
    ).toBe('de');
    expect(
      negotiateLocale({ cookieLocale: 'nonsense', acceptLanguage: 'fr' }),
    ).toBe('fr');
  });
});
