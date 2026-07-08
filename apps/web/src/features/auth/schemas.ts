import { z } from 'zod';

import type { Dictionary } from '@/lib/i18n';

/** Localized validation messages, taken from the active dictionary. */
export type AuthValidationMessages = Dictionary['auth']['validation'];

/**
 * Constraints mirror the OpenAPI contract (packages/api-client/spec):
 * email ≤ 254 (format: email), password 12..128, displayName 1..120.
 */
export function createRegisterSchema(v: AuthValidationMessages) {
  return z.object({
    displayName: z
      .string()
      .trim()
      .min(1, v.displayNameRequired)
      .max(120, v.displayNameTooLong),
    email: z
      .string()
      .trim()
      .min(1, v.emailRequired)
      .max(254, v.emailTooLong)
      .email(v.emailInvalid),
    password: z.string().min(12, v.passwordTooShort).max(128, v.passwordTooLong),
  });
}

export function createLoginSchema(v: AuthValidationMessages) {
  return z.object({
    email: z
      .string()
      .trim()
      .min(1, v.emailRequired)
      .max(254, v.emailTooLong)
      .email(v.emailInvalid),
    password: z.string().min(1, v.passwordRequired).max(128, v.passwordTooLong),
  });
}

export function createResendVerificationSchema(v: AuthValidationMessages) {
  return z.object({
    email: z
      .string()
      .trim()
      .min(1, v.emailRequired)
      .max(254, v.emailTooLong)
      .email(v.emailInvalid),
  });
}

export type RegisterFormValues = z.infer<ReturnType<typeof createRegisterSchema>>;
export type LoginFormValues = z.infer<ReturnType<typeof createLoginSchema>>;
export type ResendVerificationFormValues = z.infer<
  ReturnType<typeof createResendVerificationSchema>
>;
