import { z } from 'zod';

import type { TranslateFn } from '@/i18n/translator';

/**
 * Localized validation messages come from the `auth.validation` namespace via
 * next-intl. The builders take a translate function `(key) => string` so the
 * same schema works in Client Components (`useTranslations('auth.validation')`)
 * and in tests.
 *
 * Constraints mirror the OpenAPI contract (packages/api-client/spec):
 * email ≤ 254 (format: email), password 12..128, displayName 1..120.
 */
export function createRegisterSchema(t: TranslateFn) {
  return z.object({
    displayName: z
      .string()
      .trim()
      .min(1, t('displayNameRequired'))
      .max(120, t('displayNameTooLong')),
    email: z
      .string()
      .trim()
      .min(1, t('emailRequired'))
      .max(254, t('emailTooLong'))
      .email(t('emailInvalid')),
    password: z.string().min(12, t('passwordTooShort')).max(128, t('passwordTooLong')),
  });
}

export function createLoginSchema(t: TranslateFn) {
  return z.object({
    email: z
      .string()
      .trim()
      .min(1, t('emailRequired'))
      .max(254, t('emailTooLong'))
      .email(t('emailInvalid')),
    password: z.string().min(1, t('passwordRequired')).max(128, t('passwordTooLong')),
  });
}

export function createResendVerificationSchema(t: TranslateFn) {
  return z.object({
    email: z
      .string()
      .trim()
      .min(1, t('emailRequired'))
      .max(254, t('emailTooLong'))
      .email(t('emailInvalid')),
  });
}

export type RegisterFormValues = z.infer<ReturnType<typeof createRegisterSchema>>;
export type LoginFormValues = z.infer<ReturnType<typeof createLoginSchema>>;
export type ResendVerificationFormValues = z.infer<
  ReturnType<typeof createResendVerificationSchema>
>;
