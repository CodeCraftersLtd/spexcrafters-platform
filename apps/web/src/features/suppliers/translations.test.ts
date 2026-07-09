import { describe, expect, it } from 'vitest';

import type { ProfileTranslation } from '@spexcrafters/api-client';

import {
  isTranslationPending,
  needsStaleWarning,
  orderTranslations,
  translationIndicator,
} from './translations';

function row(overrides: Partial<ProfileTranslation>): ProfileTranslation {
  return {
    locale: 'en',
    original: false,
    translationStatus: 'APPROVED',
    translationSource: 'HUMAN',
    sourceLocale: 'en',
    sourceVersion: 1,
    stale: false,
    ...overrides,
  };
}

describe('translation indicator logic', () => {
  it('marks the original row as original regardless of stale', () => {
    expect(translationIndicator(row({ original: true, stale: true }))).toBe('original');
  });

  it('marks a non-original stale row as stale', () => {
    expect(translationIndicator(row({ original: false, stale: true }))).toBe('stale');
  });

  it('marks a non-original current row as current', () => {
    expect(translationIndicator(row({ original: false, stale: false }))).toBe('current');
  });

  it('only warns for stale non-original rows', () => {
    expect(needsStaleWarning(row({ original: false, stale: true }))).toBe(true);
    expect(needsStaleWarning(row({ original: true, stale: true }))).toBe(false);
    expect(needsStaleWarning(row({ original: false, stale: false }))).toBe(false);
  });

  it('treats machine/draft/missing translations as pending', () => {
    expect(isTranslationPending('MISSING')).toBe(true);
    expect(isTranslationPending('DRAFT')).toBe(true);
    expect(isTranslationPending('MACHINE_TRANSLATED')).toBe(true);
    expect(isTranslationPending('APPROVED')).toBe(false);
    expect(isTranslationPending('HUMAN_REVIEWED')).toBe(false);
  });

  it('orders the original first, then by locale', () => {
    const ordered = orderTranslations([
      row({ locale: 'zh-CN' }),
      row({ locale: 'ar' }),
      row({ locale: 'en', original: true }),
    ]);
    expect(ordered.map((r) => r.locale)).toEqual(['en', 'ar', 'zh-CN']);
  });
});
