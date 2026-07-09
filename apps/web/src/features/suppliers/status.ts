import type {
  ProfileTranslation,
  SupplierApplicationStatus,
  SupplierProfile,
} from '@spexcrafters/api-client';

/**
 * Application-status → design tone, for the status badge. Unknown future
 * statuses degrade to a neutral tone (clients tolerate unknown values).
 */
export type StatusTone = 'neutral' | 'info' | 'success' | 'danger' | 'warning';

const STATUS_TONE: Record<string, StatusTone> = {
  DRAFT: 'neutral',
  SUBMITTED: 'info',
  UNDER_REVIEW: 'info',
  RESUBMITTED: 'info',
  CHANGES_REQUESTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  WITHDRAWN: 'neutral',
};

export function statusTone(status: SupplierApplicationStatus): StatusTone {
  return STATUS_TONE[status] ?? 'neutral';
}

/** The original-language translation row, if present. */
export function originalTranslation(
  profile: Pick<SupplierProfile, 'translations'>,
): ProfileTranslation | undefined {
  return profile.translations.find((row) => row.original);
}

export interface CompletenessCheck {
  key: string;
  complete: boolean;
}

/**
 * Derive a completeness checklist for the review-before-submit gate. Purely
 * presentational — the backend enforces the real completeness rule on submit
 * (a 422 surfaces anything this misses). Keys map to the `suppliers.completeness`
 * namespace.
 */
export function completenessChecks(
  profile: SupplierProfile,
  evidenceCount: number,
): CompletenessCheck[] {
  const original = originalTranslation(profile);
  return [
    { key: 'legalName', complete: Boolean(profile.legalName?.trim()) },
    { key: 'registration', complete: Boolean(profile.registrationNumber?.trim()) },
    { key: 'country', complete: Boolean(profile.countryOfRegistration?.trim()) },
    { key: 'companyType', complete: Boolean(profile.companyTypeCode?.trim()) },
    { key: 'types', complete: profile.types.length > 0 },
    { key: 'capabilities', complete: profile.capabilities.length > 0 },
    {
      key: 'description',
      complete: Boolean(original?.companyDescription?.trim()),
    },
    { key: 'facilities', complete: profile.facilities.length > 0 },
    { key: 'evidence', complete: evidenceCount > 0 },
  ];
}

export function completenessRatio(checks: readonly CompletenessCheck[]): number {
  if (checks.length === 0) {
    return 0;
  }
  const done = checks.filter((c) => c.complete).length;
  return done / checks.length;
}
