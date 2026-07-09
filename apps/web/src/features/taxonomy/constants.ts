import type {
  AttributeDataType,
  BrandApprovalStatus,
  CategoryClassification,
  UnitFamily,
} from '@spexcrafters/api-client';

/**
 * Taxonomy vocabulary tuples, mirrored from the frozen OpenAPI schema
 * (packages/api-client generated types). Kept as `as const` tuples so they
 * drive both the zod `z.enum(...)` builders and the option lists rendered in
 * the admin forms. Clients must tolerate unknown future values on reads; these
 * are the write-time choices the UI offers.
 */

export const CATEGORY_CLASSIFICATIONS = [
  'LENS',
  'FRAME',
  'SUNGLASSES',
  'CONTACT_LENS',
  'MACHINERY',
  'LAB_EQUIPMENT',
  'ACCESSORY',
  'PACKAGING',
  'COMPONENT',
  'OTHER',
] as const satisfies readonly CategoryClassification[];

export const ATTRIBUTE_DATA_TYPES = [
  'STRING',
  'INTEGER',
  'DECIMAL',
  'BOOLEAN',
  'DATE',
  'ENUMERATION',
  'MEASUREMENT',
  'RANGE',
  'JSON',
  'REFERENCE',
  'MULTI_SELECT',
  'SINGLE_SELECT',
  'FILE_REFERENCE',
  'COLOR',
  'COUNTRY',
  'LANGUAGE',
  'BRAND',
  'CERTIFICATION',
] as const satisfies readonly AttributeDataType[];

/** Brand types offered at create time (BrandType in the contract). */
export const BRAND_TYPES = [
  'LENS',
  'FRAME',
  'SUNGLASSES',
  'CONTACT_LENS',
  'MACHINE',
  'ACCESSORY',
  'GENERAL',
] as const;

export type BrandType = (typeof BRAND_TYPES)[number];

export const UNIT_FAMILIES = [
  'LENGTH',
  'MASS',
  'POWER_DIOPTER',
  'ANGLE',
  'COUNT',
] as const satisfies readonly UnitFamily[];

export const CERTIFICATION_CATEGORIES = [
  'QUALITY',
  'SAFETY',
  'ENVIRONMENTAL',
  'MEDICAL',
  'REGULATORY',
] as const;

export type CertificationCategory = (typeof CERTIFICATION_CATEGORIES)[number];

/** Translation provenance (TranslationSourceKind). */
export const TRANSLATION_SOURCES = ['HUMAN', 'MACHINE', 'IMPORT'] as const;

export const BRAND_APPROVAL_STATUSES = [
  'PENDING',
  'APPROVED',
  'REJECTED',
  'DEPRECATED',
] as const satisfies readonly BrandApprovalStatus[];
