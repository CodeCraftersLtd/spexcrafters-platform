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

export type Locale = 'en' | 'zh-Hans' | 'fr' | 'de' | (string & {});

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
