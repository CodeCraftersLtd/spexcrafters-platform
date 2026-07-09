/**
 * Phase-7 taxonomy seed codes (Class-C). These are the known seed codes the
 * backend ships; the frontend renders their labels EXCLUSIVELY through the
 * `taxonomy` namespace (`taxonomy.<group>.<CODE>`) — never a hardcoded string.
 *
 * Codes are the contract; labels are localized. A code absent from the message
 * set falls back to the raw code via `taxonomyLabel` so an added backend code
 * never crashes the UI (clients tolerate unknown future values).
 */
import type { Translator } from '@/i18n/translator';

/** Company legal form (draft `companyTypeCode`). */
export const COMPANY_TYPE_CODES = [
  'LIMITED_LIABILITY',
  'JOINT_STOCK',
  'SOLE_PROPRIETORSHIP',
  'PARTNERSHIP',
  'STATE_OWNED',
] as const;

/** Employee-count band (draft `employeeRange`). */
export const EMPLOYEE_RANGE_CODES = [
  'R_1_10',
  'R_11_50',
  'R_51_200',
  'R_201_500',
  'R_501_1000',
  'R_1000_PLUS',
] as const;

// The code arrays below MIRROR the backend reference seeds exactly (V4 migration
// reference.* tables). They are the contract the backend validates against; a
// mismatch is rejected with 422. Keep in sync with the seed data until a
// reference-catalog endpoint replaces the hardcoding (tracked as Phase-7 debt).

/** Supplier type multi-select (draft `types`) — reference.supplier_type. */
export const SUPPLIER_TYPE_CODES = [
  'LENS_MANUFACTURER',
  'FRAME_MANUFACTURER',
  'CONTACT_LENS_MANUFACTURER',
  'EQUIPMENT_MANUFACTURER',
  'COMPONENT_SUPPLIER',
  'OEM_MANUFACTURER',
  'ODM_MANUFACTURER',
  'PRIVATE_LABEL_MANUFACTURER',
  'TRADING_COMPANY',
  'DISTRIBUTOR',
] as const;

/** Declared capability multi-select (draft `capabilities`) — reference.supplier_capability. */
export const CAPABILITY_CODES = [
  'LENS_COATING',
  'LENS_EDGING',
  'FRAME_ASSEMBLY',
  'INJECTION_MOLDING',
  'QUALITY_CONTROL_LAB',
  'EXPORT_LOGISTICS',
  'OEM',
  'ODM',
  'PRIVATE_LABEL',
] as const;

/** Facility type (add-facility `facilityTypeCode`) — reference.facility_type. */
export const FACILITY_TYPE_CODES = [
  'FACTORY',
  'HEADQUARTERS',
  'RESEARCH_CENTER',
  'WAREHOUSE',
  'SHOWROOM',
] as const;

/** Evidence document type (initiate-upload `evidenceTypeCode`) — reference.evidence_type. */
export const EVIDENCE_TYPE_CODES = [
  'BUSINESS_REGISTRATION_DOCUMENT',
  'BUSINESS_LICENSE',
  'TAX_CERTIFICATE',
  'ISO_CERTIFICATE',
  'QUALITY_CERTIFICATE',
  'EXPORT_LICENSE',
  'FACTORY_PHOTO',
  'OTHER',
] as const;

/** Verification scopes (grant/suspend/revoke `scopeCode`) — reference.verification_scope. */
export const VERIFICATION_SCOPE_CODES = [
  'LEGAL_ENTITY',
  'BUSINESS_REGISTRATION',
  'MANUFACTURER_STATUS',
  'OPTICAL_INDUSTRY_ACTIVITY',
  'FACTORY_EXISTENCE',
] as const;

export type TaxonomyGroup =
  | 'companyType'
  | 'employeeRange'
  | 'supplierType'
  | 'capability'
  | 'facilityType'
  | 'evidenceType'
  | 'verificationScope';

/**
 * Resolve a Class-C code to its localized label from the `taxonomy` namespace,
 * falling back to the raw code when the label is absent (unknown future code).
 * `t` is a next-intl translator scoped to `taxonomy`.
 */
export function taxonomyLabel(
  t: Translator,
  group: TaxonomyGroup,
  code: string,
): string {
  const key = `${group}.${code}`;
  return t.has(key) ? t(key) : code;
}
