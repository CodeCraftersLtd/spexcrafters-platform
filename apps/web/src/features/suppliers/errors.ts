import type { Translator } from '@/i18n/translator';

import type { BffError } from '@/features/auth/client-errors';

/**
 * Resolve a supplier/reviewer BFF error code to localized copy from the
 * `errors` namespace: a known code comes from messages, an unknown code falls
 * back to the server-provided (already locale-resolved) message, then to the
 * generic error. `t` is a next-intl translator scoped to `errors`.
 */
export function translateSupplierError(
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
