import { z } from 'zod';

import type { TranslateFn } from '@/i18n/translator';

/**
 * Reviewer decision forms. Builders take a translate function scoped to
 * `reviewer.validation`. Mirrors RequestChangesRequest / GrantScopeRequest /
 * ReasonRequest from the contract.
 */

/** Structured request-changes reason (requestedItem + reason, both required). */
export function requestChangesSchema(t: TranslateFn) {
  return z.object({
    requestedItem: z
      .string()
      .trim()
      .min(1, t('requestedItemRequired'))
      .max(200, t('tooLong')),
    reason: z.string().trim().min(1, t('reasonRequired')).max(4000, t('tooLong')),
  });
}

/** Optional free-text reason (reject / suspend / scope suspend / revoke). */
export function reasonSchema(t: TranslateFn) {
  return z.object({
    reason: z.string().trim().max(4000, t('tooLong')).or(z.literal('')).optional(),
  });
}

/**
 * Grant a verification scope. Evidence linkage is REQUIRED (min 1) — a scope
 * cannot be granted without selecting the evidence that supports it.
 */
export function grantScopeSchema(t: TranslateFn) {
  return z.object({
    evidenceIds: z
      .array(z.string())
      .min(1, t('evidenceRequired')),
    reason: z.string().trim().max(4000, t('tooLong')).or(z.literal('')).optional(),
  });
}

export type RequestChangesValues = z.infer<ReturnType<typeof requestChangesSchema>>;
export type ReasonValues = z.infer<ReturnType<typeof reasonSchema>>;
export type GrantScopeValues = z.infer<ReturnType<typeof grantScopeSchema>>;
