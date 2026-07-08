import { z } from 'zod';

import type { Dictionary } from '@/lib/i18n';

/** Localized validation messages, taken from the active dictionary. */
export type OrgValidationMessages = Dictionary['organizations']['validation'];

const ORGANIZATION_TYPES = ['BUYER', 'SUPPLIER', 'HYBRID'] as const;
const INVITABLE_ROLES = ['MEMBER', 'ADMIN'] as const;

/**
 * Optional ISO 3166-1 alpha-2 country code. Lowercase input is normalized to
 * uppercase (the contract requires uppercase); the empty string means "not
 * provided" and is dropped before the request is sent.
 */
function countryField(v: OrgValidationMessages) {
  return z
    .string()
    .trim()
    .toUpperCase()
    .regex(/^[A-Z]{2}$/, v.countryInvalid)
    .or(z.literal(''))
    .optional();
}

function nameField(v: OrgValidationMessages) {
  return z
    .string()
    .trim()
    .min(1, v.nameRequired)
    .min(2, v.nameTooShort)
    .max(120, v.nameTooLong);
}

/** Mirrors CreateOrganizationRequest: name 2..120, type enum, optional country. */
export function createOrganizationSchema(v: OrgValidationMessages) {
  return z.object({
    name: nameField(v),
    type: z.enum(ORGANIZATION_TYPES, {
      errorMap: () => ({ message: v.typeRequired }),
    }),
    country: countryField(v),
  });
}

/** Mirrors UpdateOrganizationRequest (version travels separately). */
export function updateOrganizationSchema(v: OrgValidationMessages) {
  return z.object({
    name: nameField(v),
    country: countryField(v),
  });
}

/** Mirrors CreateInvitationRequest: email + role ∈ {MEMBER, ADMIN}. */
export function inviteMemberSchema(v: OrgValidationMessages) {
  return z.object({
    email: z
      .string()
      .trim()
      .min(1, v.emailRequired)
      .max(254, v.emailTooLong)
      .email(v.emailInvalid),
    role: z.enum(INVITABLE_ROLES, {
      errorMap: () => ({ message: v.roleRequired }),
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
