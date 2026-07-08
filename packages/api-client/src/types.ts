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
