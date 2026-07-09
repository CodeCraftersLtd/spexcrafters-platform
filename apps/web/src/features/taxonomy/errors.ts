import type { Translator } from '@/i18n/translator';

import type { BffError } from '@/features/auth/client-errors';

/**
 * Resolve a taxonomy-admin BFF error code to localized copy from the
 * `errors.server` namespace: a known code comes from messages, an unknown code
 * falls back to the server-provided (already locale-resolved) message, then to
 * the generic error. `t` is a next-intl translator scoped to `errors.server`.
 * Mirrors suppliers/errors.ts so the mutation components share one shape.
 */
export function translateTaxonomyError(
  error: Pick<BffError, 'code' | 'message'>,
  t: Translator,
): string {
  if (t.has(error.code)) {
    return t(error.code);
  }
  if (error.message) {
    return error.message;
  }
  return t('unexpected');
}
