import type { ProfileTranslation, TranslationStatus } from '@spexcrafters/api-client';

/**
 * Translation-state indicator logic (framework-agnostic; unit-tested). A
 * translation row is one of three surfaced states, in priority order:
 *   - 'original'  — the authoritative original-language row (never stale)
 *   - 'stale'     — source content advanced past this translation
 *   - 'current'   — up to date with its source
 * plus the raw approval `status` used for the badge.
 */
export type TranslationIndicator = 'original' | 'stale' | 'current';

export function translationIndicator(
  translation: Pick<ProfileTranslation, 'original' | 'stale'>,
): TranslationIndicator {
  if (translation.original) {
    return 'original';
  }
  return translation.stale ? 'stale' : 'current';
}

/** A stale, non-original translation needs a re-translation warning. */
export function needsStaleWarning(
  translation: Pick<ProfileTranslation, 'original' | 'stale'>,
): boolean {
  return !translation.original && translation.stale;
}

/** Approval statuses that still require a reviewer action before they go live. */
const PENDING_STATUSES: ReadonlySet<TranslationStatus> = new Set([
  'MISSING',
  'DRAFT',
  'MACHINE_TRANSLATED',
]);

export function isTranslationPending(status: TranslationStatus): boolean {
  return PENDING_STATUSES.has(status);
}

/**
 * Order translations for display: the original first, then by locale code. The
 * original is always authoritative and leads the panel.
 */
export function orderTranslations(
  translations: readonly ProfileTranslation[],
): ProfileTranslation[] {
  return [...translations].sort((a, b) => {
    if (a.original !== b.original) {
      return a.original ? -1 : 1;
    }
    return a.locale.localeCompare(b.locale);
  });
}
