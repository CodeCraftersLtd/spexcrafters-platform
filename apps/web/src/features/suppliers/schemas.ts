import { z } from 'zod';

import type { TranslateFn } from '@/i18n/translator';

import {
  CAPABILITY_CODES,
  COMPANY_TYPE_CODES,
  EMPLOYEE_RANGE_CODES,
  FACILITY_TYPE_CODES,
  SUPPLIER_TYPE_CODES,
} from './taxonomy';

/**
 * Localized validation for the supplier onboarding forms. Builders take a
 * translate function `(key) => string` scoped to `suppliers.validation`, mirror
 * the OpenAPI request shapes, and are framework-agnostic (unit-tested with a
 * plain object literal for `t`).
 *
 * Every free-text field is optional at the draft layer (the contract's
 * UpdateSupplierDraftRequest makes all content optional; completeness is
 * enforced on submit) EXCEPT the Class-E legal name, which anchors the identity.
 */

const optionalTrimmed = (max: number, t: TranslateFn, tooLongKey: string) =>
  z
    .string()
    .trim()
    .max(max, t(tooLongKey))
    .optional()
    .or(z.literal(''));

function legalNameField(t: TranslateFn) {
  return z
    .string()
    .trim()
    .min(1, t('legalNameRequired'))
    .max(300, t('legalNameTooLong'));
}

function countryField(t: TranslateFn) {
  return z
    .string()
    .trim()
    .toUpperCase()
    .regex(/^[A-Z]{2}$/, t('countryInvalid'))
    .or(z.literal(''))
    .optional();
}

/** Create-application step: original authoring locale + Class-E legal name. */
export function createApplicationSchema(t: TranslateFn) {
  return z.object({
    originalLocale: z.string().trim().min(2, t('originalLocaleRequired')),
    legalName: legalNameField(t),
  });
}

/** Company-info draft step. `version` travels separately. */
export function companyInfoSchema(t: TranslateFn) {
  return z.object({
    legalName: legalNameField(t),
    registeredLegalNameTranslated: optionalTrimmed(300, t, 'tooLong'),
    tradingName: optionalTrimmed(300, t, 'tooLong'),
    registrationNumber: optionalTrimmed(120, t, 'tooLong'),
    countryOfRegistration: countryField(t),
    registrationAuthority: optionalTrimmed(300, t, 'tooLong'),
    registrationDate: z
      .string()
      .trim()
      .regex(/^\d{4}-\d{2}-\d{2}$/, t('dateInvalid'))
      .or(z.literal(''))
      .optional(),
    companyTypeCode: z
      .enum(COMPANY_TYPE_CODES)
      .or(z.literal(''))
      .optional(),
    yearEstablished: z
      .string()
      .trim()
      .regex(/^\d{4}$/, t('yearInvalid'))
      .or(z.literal(''))
      .optional(),
    employeeRange: z
      .enum(EMPLOYEE_RANGE_CODES)
      .or(z.literal(''))
      .optional(),
    website: z
      .string()
      .trim()
      .url(t('websiteInvalid'))
      .or(z.literal(''))
      .optional(),
    businessEmail: z
      .string()
      .trim()
      .email(t('emailInvalid'))
      .or(z.literal(''))
      .optional(),
    businessPhone: optionalTrimmed(60, t, 'tooLong'),
    types: z.array(z.enum(SUPPLIER_TYPE_CODES)),
    capabilities: z.array(z.enum(CAPABILITY_CODES)),
  });
}

/** Localized supplier content (per-locale translation upsert). */
export function translationSchema(t: TranslateFn) {
  const description = (key: string) =>
    z.string().trim().max(4000, t('tooLong')).or(z.literal('')).optional();
  return z.object({
    tradingName: optionalTrimmed(300, t, 'tooLong'),
    companyDescription: description('companyDescription'),
    productionCapabilityDescription: description('productionCapabilityDescription'),
    oemDescription: description('oemDescription'),
    odmDescription: description('odmDescription'),
    privateLabelDescription: description('privateLabelDescription'),
    qualityControlDescription: description('qualityControlDescription'),
    exportMarketDescription: description('exportMarketDescription'),
  });
}

/** Add-facility step. */
export function facilitySchema(t: TranslateFn) {
  return z.object({
    facilityTypeCode: z.enum(FACILITY_TYPE_CODES, {
      errorMap: () => ({ message: t('facilityTypeRequired') }),
    }),
    country: z
      .string()
      .trim()
      .toUpperCase()
      .regex(/^[A-Z]{2}$/, t('countryInvalid')),
    region: optionalTrimmed(200, t, 'tooLong'),
    city: optionalTrimmed(200, t, 'tooLong'),
    addressPrivacy: z.enum(['PUBLIC_CITY', 'PRIVATE'], {
      errorMap: () => ({ message: t('addressPrivacyRequired') }),
    }),
    ownership: z.enum(['OWNED', 'LEASED', 'PARTNER'], {
      errorMap: () => ({ message: t('ownershipRequired') }),
    }),
    isPublic: z.boolean(),
    name: optionalTrimmed(300, t, 'tooLong'),
    description: z.string().trim().max(4000, t('tooLong')).or(z.literal('')).optional(),
  });
}

/** Change-request response. */
export function respondChangeRequestSchema(t: TranslateFn) {
  return z.object({
    response: z
      .string()
      .trim()
      .min(1, t('responseRequired'))
      .max(4000, t('tooLong')),
  });
}

export type CreateApplicationValues = z.infer<ReturnType<typeof createApplicationSchema>>;
export type CompanyInfoValues = z.infer<ReturnType<typeof companyInfoSchema>>;
export type TranslationValues = z.infer<ReturnType<typeof translationSchema>>;
export type FacilityValues = z.infer<ReturnType<typeof facilitySchema>>;
export type RespondChangeRequestValues = z.infer<
  ReturnType<typeof respondChangeRequestSchema>
>;
