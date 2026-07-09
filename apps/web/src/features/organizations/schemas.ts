import { z } from 'zod';

import type { TranslateFn } from '@/i18n/translator';

const ORGANIZATION_TYPES = ['BUYER', 'SUPPLIER', 'HYBRID'] as const;
const INVITABLE_ROLES = ['MEMBER', 'ADMIN'] as const;

/**
 * Localized validation messages come from the `organizations.validation`
 * namespace via next-intl. Builders take a translate function `(key) => string`.
 *
 * Optional ISO 3166-1 alpha-2 country code. Lowercase input is normalized to
 * uppercase (the contract requires uppercase); the empty string means "not
 * provided" and is dropped before the request is sent.
 */
function countryField(t: TranslateFn) {
  return z
    .string()
    .trim()
    .toUpperCase()
    .regex(/^[A-Z]{2}$/, t('countryInvalid'))
    .or(z.literal(''))
    .optional();
}

function nameField(t: TranslateFn) {
  return z
    .string()
    .trim()
    .min(1, t('nameRequired'))
    .min(2, t('nameTooShort'))
    .max(120, t('nameTooLong'));
}

/** Mirrors CreateOrganizationRequest: name 2..120, type enum, optional country. */
export function createOrganizationSchema(t: TranslateFn) {
  return z.object({
    name: nameField(t),
    type: z.enum(ORGANIZATION_TYPES, {
      errorMap: () => ({ message: t('typeRequired') }),
    }),
    country: countryField(t),
  });
}

/** Mirrors UpdateOrganizationRequest (version travels separately). */
export function updateOrganizationSchema(t: TranslateFn) {
  return z.object({
    name: nameField(t),
    country: countryField(t),
  });
}

/** Mirrors CreateInvitationRequest: email + role ∈ {MEMBER, ADMIN}. */
export function inviteMemberSchema(t: TranslateFn) {
  return z.object({
    email: z
      .string()
      .trim()
      .min(1, t('emailRequired'))
      .max(254, t('emailTooLong'))
      .email(t('emailInvalid')),
    role: z.enum(INVITABLE_ROLES, {
      errorMap: () => ({ message: t('roleRequired') }),
    }),
  });
}

export type CreateOrganizationFormValues = z.infer<
  ReturnType<typeof createOrganizationSchema>
>;
export type UpdateOrganizationFormValues = z.infer<
  ReturnType<typeof updateOrganizationSchema>
>;
export type InviteMemberFormValues = z.infer<ReturnType<typeof inviteMemberSchema>>;
