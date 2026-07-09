/**
 * Bootstrap types for the Sprint-1 API surface.
 *
 * These mirror spec/openapi.json exactly and exist only because the initial
 * scaffold was authored on a machine without a Node toolchain. The CI
 * `contract` job runs `pnpm --filter @spexcrafters/api-client generate` and
 * typechecks these aliases against the generated schema; once the first
 * generation lands, `generated/schema.d.ts` becomes the source and this file
 * must reduce to re-exports. Do not add hand-written shapes for new endpoints.
 */

import type { components } from './generated/schema';

export type Locale = 'en' | 'zh-Hans' | 'fr' | 'de' | (string & {});

// ---------------------------------------------------------------------------
// Organizations (Phase 5) — aliases over the generated OpenAPI schema.
// These are re-exports, not hand-written shapes: the generated schema.d.ts is
// the source of truth for every organizations endpoint.
// ---------------------------------------------------------------------------

export type OrganizationType = components['schemas']['OrganizationType'];
export type OrganizationRole = components['schemas']['OrganizationRole'];
export type Capability = components['schemas']['Capability'];
export type InvitationStatus = components['schemas']['InvitationStatus'];
export type CreateOrganizationRequest = components['schemas']['CreateOrganizationRequest'];
export type UpdateOrganizationRequest = components['schemas']['UpdateOrganizationRequest'];
export type OrganizationResponse = components['schemas']['OrganizationResponse'];
export type MyMembership = components['schemas']['MyMembership'];
export type MemberResponse = components['schemas']['MemberResponse'];
export type ChangeRoleRequest = components['schemas']['ChangeRoleRequest'];
export type CreateInvitationRequest = components['schemas']['CreateInvitationRequest'];
export type InvitationResponse = components['schemas']['InvitationResponse'];
export type AcceptInvitationRequest = components['schemas']['AcceptInvitationRequest'];

// ---------------------------------------------------------------------------
// Supplier / verification / evidence / reviewer (Phase 7) — aliases over the
// generated OpenAPI schema. Re-exports only: schema.d.ts is the source of truth.
// ---------------------------------------------------------------------------

export type LocaleInfo = components['schemas']['LocaleInfo'];

// Enumerations (clients must tolerate unknown future values).
export type SupplierApplicationStatus = components['schemas']['SupplierApplicationStatus'];
export type OperationalStatus = components['schemas']['OperationalStatus'];
export type ClaimStatus = components['schemas']['ClaimStatus'];
export type TranslationStatus = components['schemas']['TranslationStatus'];
export type TranslationSource = components['schemas']['TranslationSource'];
export type ScanStatus = components['schemas']['ScanStatus'];
export type EvidenceUploadState = components['schemas']['EvidenceUploadState'];
export type EvidenceReviewStatus = components['schemas']['EvidenceReviewStatus'];
export type RetentionStatus = components['schemas']['RetentionStatus'];
export type VerificationScopeStatus = components['schemas']['VerificationScopeStatus'];
export type AddressPrivacy = components['schemas']['AddressPrivacy'];
export type FacilityOwnership = components['schemas']['FacilityOwnership'];
export type SupplierCapability = components['schemas']['SupplierCapability'];

// Supplier application + profile.
export type CreateSupplierApplicationRequest =
  components['schemas']['CreateSupplierApplicationRequest'];
export type SupplierApplication = components['schemas']['SupplierApplication'];
export type UpdateSupplierDraftRequest = components['schemas']['UpdateSupplierDraftRequest'];
export type RespondChangeRequestRequest =
  components['schemas']['RespondChangeRequestRequest'];
export type ReviewRequest = components['schemas']['ReviewRequest'];
export type CapabilityDeclaration = components['schemas']['CapabilityDeclaration'];
export type ProfileTranslation = components['schemas']['ProfileTranslation'];
export type FacilityTranslation = components['schemas']['FacilityTranslation'];
export type Facility = components['schemas']['Facility'];
export type SupplierProfile = components['schemas']['SupplierProfile'];
export type UpsertTranslationRequest = components['schemas']['UpsertTranslationRequest'];
export type AddFacilityRequest = components['schemas']['AddFacilityRequest'];

// Evidence.
export type InitiateUploadRequest = components['schemas']['InitiateUploadRequest'];
export type EvidenceUploadTicket = components['schemas']['EvidenceUploadTicket'];
export type FinalizeUploadRequest = components['schemas']['FinalizeUploadRequest'];
export type Evidence = components['schemas']['Evidence'];

// Verification.
export type VerificationScopeResult = components['schemas']['VerificationScopeResult'];
export type VerificationStatus = components['schemas']['VerificationStatus'];
export type GrantScopeRequest = components['schemas']['GrantScopeRequest'];

// Reviewer / platform moderation.
export type ReasonRequest = components['schemas']['ReasonRequest'];
export type RequestChangesRequest = components['schemas']['RequestChangesRequest'];
export type ReviewQueueItem = components['schemas']['ReviewQueueItem'];
export type ReviewQueuePage = components['schemas']['ReviewQueuePage'];
export type ReviewDetail = components['schemas']['ReviewDetail'];

// Public profile foundation.
export type PublicSupplierProfile = components['schemas']['PublicSupplierProfile'];

// ---------------------------------------------------------------------------
// Optical taxonomy & specification registry (Phase 8) — aliases over the
// generated OpenAPI schema. Re-exports only: schema.d.ts is the source of
// truth. See docs/architecture/optical-taxonomy-domain-model.md and
// ADR-024..027.
// ---------------------------------------------------------------------------

// Enumerations (clients must tolerate unknown future values).
export type CategoryClassification = components['schemas']['CategoryClassification'];
export type AttributeDataType = components['schemas']['AttributeDataType'];
export type UnitFamily = components['schemas']['UnitFamily'];
export type BrandType = components['schemas']['BrandType'];
export type BrandApprovalStatus = components['schemas']['BrandApprovalStatus'];
export type TranslationSourceKind = components['schemas']['TranslationSourceKind'];
export type TaxonomyCapability = components['schemas']['TaxonomyCapability'];

// Categories.
export type CategoryTreeNode = components['schemas']['CategoryTreeNode'];
export type CategoryDetail = components['schemas']['CategoryDetail'];
export type CreateCategoryRequest = components['schemas']['CreateCategoryRequest'];
export type UpdateCategoryRequest = components['schemas']['UpdateCategoryRequest'];
export type ActivationRequest = components['schemas']['ActivationRequest'];

// Attributes.
export type AttributeSummary = components['schemas']['AttributeSummary'];
export type AttributeDetail = components['schemas']['AttributeDetail'];
export type CreateAttributeRequest = components['schemas']['CreateAttributeRequest'];
export type UpdateAttributeRequest = components['schemas']['UpdateAttributeRequest'];
export type DeprecationRequest = components['schemas']['DeprecationRequest'];

// Enumerations registry.
export type EnumerationSummary = components['schemas']['EnumerationSummary'];
export type EnumerationDetail = components['schemas']['EnumerationDetail'];
export type EnumerationValueView = components['schemas']['EnumerationValueView'];
export type CreateEnumerationRequest = components['schemas']['CreateEnumerationRequest'];
export type AddEnumerationValueRequest = components['schemas']['AddEnumerationValueRequest'];

// Units, countries, certifications.
export type Unit = components['schemas']['Unit'];
export type CreateUnitRequest = components['schemas']['CreateUnitRequest'];
export type Country = components['schemas']['Country'];
export type Certification = components['schemas']['Certification'];
export type CreateCertificationRequest = components['schemas']['CreateCertificationRequest'];

// Brands.
export type BrandSummary = components['schemas']['BrandSummary'];
export type BrandDetail = components['schemas']['BrandDetail'];
export type CreateBrandRequest = components['schemas']['CreateBrandRequest'];
export type BrandApprovalRequest = components['schemas']['BrandApprovalRequest'];

// Translations (shared across category/attribute/enumeration-value/brand).
export type TranslationUpsertRequest = components['schemas']['TranslationUpsertRequest'];
export type TranslationView = components['schemas']['TranslationView'];

// Specification templates & validation.
export type SpecificationTemplateAttributeView =
  components['schemas']['SpecificationTemplateAttributeView'];
export type EffectiveSpecificationTemplate =
  components['schemas']['EffectiveSpecificationTemplate'];
export type PutSpecificationTemplateRequest =
  components['schemas']['PutSpecificationTemplateRequest'];
export type SpecificationValidationRequest =
  components['schemas']['SpecificationValidationRequest'];
export type SpecificationValidationResult =
  components['schemas']['SpecificationValidationResult'];

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
  locale?: Locale;
}

export interface RegisterResponse {
  userId: string;
}

export interface VerifyEmailRequest {
  token: string;
}

export interface ResendVerificationRequest {
  email: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface UserSummary {
  id: string;
  email: string;
  displayName: string;
  locale: Locale;
  emailVerified: boolean;
  createdAt: string;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  refreshToken: string;
  user: UserSummary;
}

export interface ProblemFieldError {
  field: string;
  code: string;
  message: string;
}

/** RFC 9457 problem details, content type application/problem+json. */
export interface Problem {
  type: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  correlationId?: string;
  errors?: ProblemFieldError[];
}
