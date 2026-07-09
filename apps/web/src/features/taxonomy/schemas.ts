import { z } from 'zod';

import type { TranslateFn } from '@/i18n/translator';

import {
  ATTRIBUTE_DATA_TYPES,
  BRAND_TYPES,
  CATEGORY_CLASSIFICATIONS,
  CERTIFICATION_CATEGORIES,
  TRANSLATION_SOURCES,
  UNIT_FAMILIES,
} from './constants';

/**
 * Localized validation for the taxonomy admin forms. Every builder takes a
 * translate function `(key) => string` scoped to `taxonomyAdmin.validation`,
 * mirrors the frozen request DTO shapes (packages/api-client), and is
 * framework-agnostic (unit-tested with a plain `(key) => key` echo function).
 *
 * Numeric inputs are modelled as validated strings (react-hook-form registers
 * text inputs as strings); the components convert non-empty values to numbers
 * before sending, matching the Phase-7 supplier-form convention.
 */

/** Uppercase code: identity, immutable. Letters, digits, underscores. */
function codeField(t: TranslateFn) {
  return z
    .string()
    .trim()
    .min(1, t('codeRequired'))
    .max(120, t('tooLong'))
    .regex(/^[A-Z][A-Z0-9_]*$/, t('codeInvalid'));
}

/** Unit codes are short symbols (mm, D, °) — a looser identifier rule. */
function unitCodeField(t: TranslateFn) {
  return z
    .string()
    .trim()
    .min(1, t('codeRequired'))
    .max(60, t('tooLong'))
    .regex(/^[A-Za-z0-9_]+$/, t('unitCodeInvalid'));
}

function nameField(t: TranslateFn, key = 'nameRequired') {
  return z.string().trim().min(1, t(key)).max(300, t('tooLong'));
}

const optionalTrimmed = (max: number, t: TranslateFn) =>
  z.string().trim().max(max, t('tooLong')).optional().or(z.literal(''));

const optionalCode = () =>
  z
    .string()
    .trim()
    .toUpperCase()
    .optional()
    .or(z.literal(''));

const optionalInt = (t: TranslateFn) =>
  z
    .string()
    .trim()
    .regex(/^\d+$/, t('sortOrderInvalid'))
    .optional()
    .or(z.literal(''));

const optionalDecimal = (t: TranslateFn) =>
  z
    .string()
    .trim()
    .regex(/^-?\d+(\.\d+)?$/, t('numberInvalid'))
    .optional()
    .or(z.literal(''));

function localeField(t: TranslateFn, key = 'originalLocaleRequired') {
  return z.string().trim().min(2, t(key));
}

function optionalCountry(t: TranslateFn) {
  return z
    .string()
    .trim()
    .toUpperCase()
    .regex(/^[A-Z]{2}$/, t('countryInvalid'))
    .optional()
    .or(z.literal(''));
}

/** createCategory — CreateCategoryRequest. */
export function createCategorySchema(t: TranslateFn) {
  return z.object({
    code: codeField(t),
    parentCode: optionalCode(),
    classification: z.enum(CATEGORY_CLASSIFICATIONS, {
      errorMap: () => ({ message: t('classificationRequired') }),
    }),
    originalLocale: localeField(t),
    name: nameField(t),
    sortOrder: optionalInt(t),
  });
}

/** createAttribute — CreateAttributeRequest (form subset of the flags). */
export function createAttributeSchema(t: TranslateFn) {
  return z.object({
    code: codeField(t),
    dataType: z.enum(ATTRIBUTE_DATA_TYPES, {
      errorMap: () => ({ message: t('dataTypeRequired') }),
    }),
    unitCode: optionalCode(),
    enumerationCode: optionalCode(),
    originalLocale: localeField(t),
    name: nameField(t),
    description: optionalTrimmed(4000, t),
    searchable: z.boolean(),
    filterable: z.boolean(),
    visible: z.boolean(),
  });
}

/** createEnumeration — CreateEnumerationRequest. */
export function createEnumerationSchema(t: TranslateFn) {
  return z.object({
    code: codeField(t),
  });
}

/** addEnumerationValue — AddEnumerationValueRequest. */
export function addEnumerationValueSchema(t: TranslateFn) {
  return z.object({
    code: codeField(t),
    label: nameField(t, 'labelRequired'),
    originalLocale: localeField(t),
    sortOrder: optionalInt(t),
    description: optionalTrimmed(4000, t),
  });
}

/** createBrand — CreateBrandRequest. */
export function createBrandSchema(t: TranslateFn) {
  return z.object({
    code: codeField(t),
    brandType: z.enum(BRAND_TYPES, {
      errorMap: () => ({ message: t('brandTypeRequired') }),
    }),
    canonicalName: nameField(t, 'canonicalNameRequired'),
    displayName: optionalTrimmed(300, t),
    ownerCompany: optionalTrimmed(300, t),
    manufacturer: optionalTrimmed(300, t),
    countryCode: optionalCountry(t),
    website: z.string().trim().url(t('websiteInvalid')).optional().or(z.literal('')),
    originalLocale: localeField(t),
  });
}

/** createCertification — CreateCertificationRequest. */
export function createCertificationSchema(t: TranslateFn) {
  return z.object({
    code: codeField(t),
    category: z.enum(CERTIFICATION_CATEGORIES).optional().or(z.literal('')),
    countryScope: optionalCountry(t),
    validityMonths: optionalInt(t),
    originalLocale: localeField(t),
    name: nameField(t),
    description: optionalTrimmed(4000, t),
  });
}

/** createUnit — CreateUnitRequest. */
export function createUnitSchema(t: TranslateFn) {
  return z.object({
    code: unitCodeField(t),
    family: z.enum(UNIT_FAMILIES, {
      errorMap: () => ({ message: t('familyRequired') }),
    }),
    baseUnitCode: optionalCode(),
    factorToBase: optionalDecimal(t),
    originalLocale: localeField(t),
    displayName: nameField(t, 'displayNameRequired'),
  });
}

/** putSpecificationTemplate — PutSpecificationTemplateRequest. */
export function specificationTemplateSchema(t: TranslateFn) {
  return z.object({
    categoryId: z.string().trim().min(1, t('classificationRequired')),
    code: codeField(t),
    attributes: z
      .array(
        z.object({
          attributeCode: z.string().trim().min(1, t('attributeRequired')),
          required: z.boolean(),
          sortOrder: optionalInt(t),
          defaultValue: optionalTrimmed(300, t),
        }),
      )
      .min(1, t('atLeastOneAttribute')),
  });
}

/** translation upsert — TranslationUpsertRequest (name + optional description). */
export function translationUpsertSchema(t: TranslateFn) {
  return z.object({
    locale: localeField(t, 'localeRequired'),
    name: nameField(t),
    description: optionalTrimmed(4000, t),
    source: z.enum(TRANSLATION_SOURCES),
  });
}

export type CreateCategoryValues = z.infer<ReturnType<typeof createCategorySchema>>;
export type CreateAttributeValues = z.infer<ReturnType<typeof createAttributeSchema>>;
export type CreateEnumerationValues = z.infer<
  ReturnType<typeof createEnumerationSchema>
>;
export type AddEnumerationValueValues = z.infer<
  ReturnType<typeof addEnumerationValueSchema>
>;
export type CreateBrandValues = z.infer<ReturnType<typeof createBrandSchema>>;
export type CreateCertificationValues = z.infer<
  ReturnType<typeof createCertificationSchema>
>;
export type CreateUnitValues = z.infer<ReturnType<typeof createUnitSchema>>;
export type SpecificationTemplateValues = z.infer<
  ReturnType<typeof specificationTemplateSchema>
>;
export type TranslationUpsertValues = z.infer<
  ReturnType<typeof translationUpsertSchema>
>;
