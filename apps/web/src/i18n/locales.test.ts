import { describe, expect, it } from 'vitest';

import {
  DEFAULT_LOCALE,
  LOCALES,
  LOCALE_ENDONYMS,
  RTL_LOCALES,
  dirOf,
  isSupportedLocale,
  normalizeLocale,
} from './locales';

describe('locale registry', () => {
  it('registers exactly the 20 launch locales, en first', () => {
    expect(LOCALES).toHaveLength(20);
    expect(LOCALES[0]).toBe('en');
    expect(new Set(LOCALES).size).toBe(20);
    for (const code of ['en', 'zh-CN', 'ko', 'ja', 'ar', 'fa', 'ur', 'de', 'pt', 'fil']) {
      expect(LOCALES).toContain(code);
    }
  });

  it('uses en as the deterministic default', () => {
    expect(DEFAULT_LOCALE).toBe('en');
  });

  it('declares ar/fa/ur as the RTL set', () => {
    expect([...RTL_LOCALES].sort()).toEqual(['ar', 'fa', 'ur']);
  });

  it('has an endonym for every registered locale', () => {
    for (const locale of LOCALES) {
      expect(LOCALE_ENDONYMS[locale]).toBeTruthy();
    }
    expect(LOCALE_ENDONYMS.de).toBe('Deutsch');
    expect(LOCALE_ENDONYMS['zh-CN']).toBe('简体中文');
  });
});

describe('isSupportedLocale', () => {
  it('accepts canonical codes only', () => {
    expect(isSupportedLocale('en')).toBe(true);
    expect(isSupportedLocale('zh-CN')).toBe(true);
    expect(isSupportedLocale('ar')).toBe(true);
  });

  it('rejects aliases, wrong case, and unknowns', () => {
    expect(isSupportedLocale('zh-Hans')).toBe(false);
    expect(isSupportedLocale('EN')).toBe(false);
    expect(isSupportedLocale('xx')).toBe(false);
    expect(isSupportedLocale('')).toBe(false);
  });
});

describe('normalizeLocale', () => {
  it('resolves the zh alias family to zh-CN', () => {
    expect(normalizeLocale('zh')).toBe('zh-CN');
    expect(normalizeLocale('zh-Hans')).toBe('zh-CN');
    expect(normalizeLocale('ZH-HANS')).toBe('zh-CN');
    expect(normalizeLocale('zh-SG')).toBe('zh-CN');
    expect(normalizeLocale('zh-TW')).toBe('zh-CN');
  });

  it('matches case-insensitively to the canonical code', () => {
    expect(normalizeLocale('DE')).toBe('de');
    expect(normalizeLocale('Fr')).toBe('fr');
    expect(normalizeLocale('AR')).toBe('ar');
  });

  it('falls back to the base language for regional tags', () => {
    expect(normalizeLocale('en-GB')).toBe('en');
    expect(normalizeLocale('fr-CA')).toBe('fr');
    expect(normalizeLocale('pt-BR')).toBe('pt');
    expect(normalizeLocale('de-AT')).toBe('de');
  });

  it('resolves unknown, empty, and x-default deterministically to en', () => {
    expect(normalizeLocale('xx')).toBe('en');
    expect(normalizeLocale('x-default')).toBe('en');
    expect(normalizeLocale('*')).toBe('en');
    expect(normalizeLocale('')).toBe('en');
    expect(normalizeLocale(null)).toBe('en');
    expect(normalizeLocale(undefined)).toBe('en');
  });

  it('maps the legacy Indonesian code', () => {
    expect(normalizeLocale('in')).toBe('id');
  });
});

describe('dirOf', () => {
  it('returns rtl for the RTL set', () => {
    for (const rtl of ['ar', 'fa', 'ur']) {
      expect(dirOf(rtl)).toBe('rtl');
    }
  });

  it('returns ltr for everything else, including unknown input', () => {
    for (const ltr of ['en', 'zh-CN', 'de', 'ru', 'hi', 'th']) {
      expect(dirOf(ltr)).toBe('ltr');
    }
    expect(dirOf('unknown')).toBe('ltr');
  });
});
