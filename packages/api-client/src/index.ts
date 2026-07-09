import type {
  AcceptInvitationRequest,
  AddFacilityRequest,
  ChangeRoleRequest,
  CreateInvitationRequest,
  CreateOrganizationRequest,
  CreateSupplierApplicationRequest,
  Evidence,
  EvidenceUploadTicket,
  Facility,
  FinalizeUploadRequest,
  GrantScopeRequest,
  InitiateUploadRequest,
  InvitationResponse,
  LocaleInfo,
  LoginRequest,
  MemberResponse,
  MyMembership,
  OrganizationResponse,
  Problem,
  ProfileTranslation,
  PublicSupplierProfile,
  ReasonRequest,
  RefreshRequest,
  RegisterRequest,
  RegisterResponse,
  RequestChangesRequest,
  ResendVerificationRequest,
  RespondChangeRequestRequest,
  ReviewDetail,
  ReviewQueuePage,
  ReviewRequest,
  SupplierApplication,
  SupplierProfile,
  TokenResponse,
  UpdateOrganizationRequest,
  UpdateSupplierDraftRequest,
  UpsertTranslationRequest,
  UserSummary,
  VerificationStatus,
  VerifyEmailRequest,
} from './types';

export * from './types';

/** Thrown for any non-2xx response; carries the parsed RFC 9457 problem. */
export class ApiProblemError extends Error {
  readonly problem: Problem;

  constructor(problem: Problem) {
    super(`${problem.status} ${problem.title}`);
    this.name = 'ApiProblemError';
    this.problem = problem;
  }
}

export interface ApiClientOptions {
  /** e.g. http://localhost:8080/api/v1 (no trailing slash) */
  baseUrl: string;
  /** Supplies the bearer token for authenticated calls. */
  getAccessToken?: () => string | undefined;
  fetchImpl?: typeof fetch;
}

async function parseProblem(response: Response): Promise<Problem> {
  try {
    const body = (await response.json()) as Partial<Problem>;
    return {
      type: body.type ?? 'about:blank',
      title: body.title ?? response.statusText,
      status: body.status ?? response.status,
      ...body,
    } as Problem;
  } catch {
    return { type: 'about:blank', title: response.statusText, status: response.status };
  }
}

/**
 * Minimal typed client for the Sprint-1 surface. Server-side use only
 * (Next.js BFF route handlers); the browser never calls the API directly.
 */
export function createApiClient(options: ApiClientOptions) {
  const { baseUrl, getAccessToken, fetchImpl = fetch } = options;

  async function request<T>(
    method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE',
    path: string,
    body?: unknown,
    authenticated = false,
  ): Promise<T> {
    const headers: Record<string, string> = { Accept: 'application/json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (authenticated) {
      const token = getAccessToken?.();
      if (token) headers.Authorization = `Bearer ${token}`;
    }
    const init: RequestInit = { method, headers, cache: 'no-store' };
    if (body !== undefined) init.body = JSON.stringify(body);
    const response = await fetchImpl(`${baseUrl}${path}`, init);
    if (!response.ok) throw new ApiProblemError(await parseProblem(response));
    if (response.status === 204 || response.status === 202) return undefined as T;
    return (await response.json()) as T;
  }

  /**
   * Authenticated GET that returns the raw `Response` (no JSON parsing) for
   * binary/streamed payloads — the BFF download proxy pipes the body straight
   * back to the browser and forwards the scan-state header.
   */
  async function requestRaw(path: string): Promise<Response> {
    const headers: Record<string, string> = {};
    const token = getAccessToken?.();
    if (token) headers.Authorization = `Bearer ${token}`;
    const response = await fetchImpl(`${baseUrl}${path}`, {
      method: 'GET',
      headers,
      cache: 'no-store',
    });
    if (!response.ok) throw new ApiProblemError(await parseProblem(response));
    return response;
  }

  const enc = encodeURIComponent;

  return {
    register: (req: RegisterRequest) => request<RegisterResponse>('POST', '/auth/register', req),
    verifyEmail: (req: VerifyEmailRequest) => request<void>('POST', '/auth/verify-email', req),
    resendVerification: (req: ResendVerificationRequest) =>
      request<void>('POST', '/auth/resend-verification', req),
    login: (req: LoginRequest) => request<TokenResponse>('POST', '/auth/login', req),
    refreshTokens: (req: RefreshRequest) => request<TokenResponse>('POST', '/auth/refresh', req),
    logout: (req: RefreshRequest) => request<void>('POST', '/auth/logout', req),
    getCurrentUser: () => request<UserSummary>('GET', '/me', undefined, true),

    // Organizations (Phase 5) — all authenticated (bearer).
    listMyOrganizations: () =>
      request<MyMembership[]>('GET', '/me/organizations', undefined, true),
    createOrganization: (req: CreateOrganizationRequest) =>
      request<OrganizationResponse>('POST', '/organizations', req, true),
    getOrganization: (organizationId: string) =>
      request<OrganizationResponse>(
        'GET',
        `/organizations/${encodeURIComponent(organizationId)}`,
        undefined,
        true,
      ),
    updateOrganization: (organizationId: string, req: UpdateOrganizationRequest) =>
      request<OrganizationResponse>(
        'PATCH',
        `/organizations/${encodeURIComponent(organizationId)}`,
        req,
        true,
      ),
    listMembers: (organizationId: string) =>
      request<MemberResponse[]>(
        'GET',
        `/organizations/${encodeURIComponent(organizationId)}/members`,
        undefined,
        true,
      ),
    removeMember: (organizationId: string, membershipId: string) =>
      request<void>(
        'DELETE',
        `/organizations/${encodeURIComponent(organizationId)}/members/${encodeURIComponent(membershipId)}`,
        undefined,
        true,
      ),
    changeMemberRole: (
      organizationId: string,
      membershipId: string,
      req: ChangeRoleRequest,
    ) =>
      request<MemberResponse>(
        'PUT',
        `/organizations/${encodeURIComponent(organizationId)}/members/${encodeURIComponent(membershipId)}/role`,
        req,
        true,
      ),
    listInvitations: (organizationId: string) =>
      request<InvitationResponse[]>(
        'GET',
        `/organizations/${encodeURIComponent(organizationId)}/invitations`,
        undefined,
        true,
      ),
    createInvitation: (organizationId: string, req: CreateInvitationRequest) =>
      request<InvitationResponse>(
        'POST',
        `/organizations/${encodeURIComponent(organizationId)}/invitations`,
        req,
        true,
      ),
    revokeInvitation: (organizationId: string, invitationId: string) =>
      request<void>(
        'POST',
        `/organizations/${encodeURIComponent(organizationId)}/invitations/${encodeURIComponent(invitationId)}/revoke`,
        undefined,
        true,
      ),
    acceptInvitation: (req: AcceptInvitationRequest) =>
      request<MyMembership>('POST', '/invitations/accept', req, true),

    // -----------------------------------------------------------------------
    // Reference / localization (Phase 7) — public, no bearer.
    // -----------------------------------------------------------------------
    listLocales: () => request<LocaleInfo[]>('GET', '/locales'),
    getPublicSupplierProfileFoundation: (supplierId: string, locale?: string) =>
      request<PublicSupplierProfile>(
        'GET',
        `/public/suppliers/${enc(supplierId)}/profile-foundation${
          locale ? `?locale=${enc(locale)}` : ''
        }`,
      ),

    // -----------------------------------------------------------------------
    // Supplier onboarding (Phase 7) — org-scoped, bearer.
    // -----------------------------------------------------------------------
    createSupplierApplication: (req: CreateSupplierApplicationRequest) =>
      request<SupplierApplication>('POST', '/suppliers/applications', req, true),
    getSupplierApplication: (applicationId: string) =>
      request<SupplierApplication>(
        'GET',
        `/suppliers/applications/${enc(applicationId)}`,
        undefined,
        true,
      ),
    updateSupplierApplicationDraft: (
      applicationId: string,
      req: UpdateSupplierDraftRequest,
    ) =>
      request<SupplierApplication>(
        'PATCH',
        `/suppliers/applications/${enc(applicationId)}`,
        req,
        true,
      ),
    submitSupplierApplication: (applicationId: string) =>
      request<SupplierApplication>(
        'POST',
        `/suppliers/applications/${enc(applicationId)}/submit`,
        undefined,
        true,
      ),
    withdrawSupplierApplication: (applicationId: string) =>
      request<SupplierApplication>(
        'POST',
        `/suppliers/applications/${enc(applicationId)}/withdraw`,
        undefined,
        true,
      ),
    listChangeRequests: (applicationId: string) =>
      request<ReviewRequest[]>(
        'GET',
        `/suppliers/applications/${enc(applicationId)}/change-requests`,
        undefined,
        true,
      ),
    respondToChangeRequest: (
      applicationId: string,
      changeRequestId: string,
      req: RespondChangeRequestRequest,
    ) =>
      request<ReviewRequest>(
        'POST',
        `/suppliers/applications/${enc(applicationId)}/change-requests/${enc(
          changeRequestId,
        )}/respond`,
        req,
        true,
      ),

    // Profile + translations + facilities.
    getSupplierProfile: (supplierId: string) =>
      request<SupplierProfile>(
        'GET',
        `/suppliers/${enc(supplierId)}/profile`,
        undefined,
        true,
      ),
    upsertProfileTranslation: (
      supplierId: string,
      locale: string,
      req: UpsertTranslationRequest,
    ) =>
      request<ProfileTranslation>(
        'PUT',
        `/suppliers/${enc(supplierId)}/profile/translations/${enc(locale)}`,
        req,
        true,
      ),
    approveProfileTranslation: (supplierId: string, locale: string) =>
      request<ProfileTranslation>(
        'POST',
        `/suppliers/${enc(supplierId)}/profile/translations/${enc(locale)}/approve`,
        undefined,
        true,
      ),
    rejectProfileTranslation: (supplierId: string, locale: string) =>
      request<ProfileTranslation>(
        'POST',
        `/suppliers/${enc(supplierId)}/profile/translations/${enc(locale)}/reject`,
        undefined,
        true,
      ),
    addSupplierFacility: (supplierId: string, req: AddFacilityRequest) =>
      request<Facility>(
        'POST',
        `/suppliers/${enc(supplierId)}/facilities`,
        req,
        true,
      ),

    // Evidence.
    listEvidence: (supplierId: string) =>
      request<Evidence[]>(
        'GET',
        `/suppliers/${enc(supplierId)}/evidence`,
        undefined,
        true,
      ),
    initiateEvidenceUpload: (supplierId: string, req: InitiateUploadRequest) =>
      request<EvidenceUploadTicket>(
        'POST',
        `/suppliers/${enc(supplierId)}/evidence/initiate-upload`,
        req,
        true,
      ),
    finalizeEvidenceUpload: (
      supplierId: string,
      evidenceId: string,
      req?: FinalizeUploadRequest,
    ) =>
      request<Evidence>(
        'POST',
        `/suppliers/${enc(supplierId)}/evidence/${enc(evidenceId)}/finalize`,
        req ?? {},
        true,
      ),
    deleteEvidence: (supplierId: string, evidenceId: string) =>
      request<void>(
        'DELETE',
        `/suppliers/${enc(supplierId)}/evidence/${enc(evidenceId)}`,
        undefined,
        true,
      ),
    /** Raw authorized stream — the BFF proxies the body and scan-state header. */
    downloadEvidence: (supplierId: string, evidenceId: string) =>
      requestRaw(`/suppliers/${enc(supplierId)}/evidence/${enc(evidenceId)}/download`),

    // Verification (scope-based; never a boolean).
    getVerificationStatus: (supplierId: string) =>
      request<VerificationStatus>(
        'GET',
        `/suppliers/${enc(supplierId)}/verification`,
        undefined,
        true,
      ),

    // -----------------------------------------------------------------------
    // Reviewer / platform moderation (Phase 7) — platform-staff, bearer.
    // -----------------------------------------------------------------------
    listReviewQueue: (params?: { cursor?: string; size?: number }) => {
      const query = new URLSearchParams();
      if (params?.cursor) query.set('cursor', params.cursor);
      if (params?.size !== undefined) query.set('size', String(params.size));
      const qs = query.toString();
      return request<ReviewQueuePage>(
        'GET',
        `/platform/review/suppliers${qs ? `?${qs}` : ''}`,
        undefined,
        true,
      );
    },
    getReviewDetail: (applicationId: string) =>
      request<ReviewDetail>(
        'GET',
        `/platform/review/suppliers/${enc(applicationId)}`,
        undefined,
        true,
      ),
    claimReview: (applicationId: string) =>
      request<ReviewDetail>(
        'POST',
        `/platform/review/suppliers/${enc(applicationId)}/claim`,
        undefined,
        true,
      ),
    requestSupplierChanges: (applicationId: string, req: RequestChangesRequest) =>
      request<ReviewDetail>(
        'POST',
        `/platform/review/suppliers/${enc(applicationId)}/request-changes`,
        req,
        true,
      ),
    approveSupplierApplication: (applicationId: string) =>
      request<ReviewDetail>(
        'POST',
        `/platform/review/suppliers/${enc(applicationId)}/approve`,
        undefined,
        true,
      ),
    rejectSupplierApplication: (applicationId: string, req?: ReasonRequest) =>
      request<ReviewDetail>(
        'POST',
        `/platform/review/suppliers/${enc(applicationId)}/reject`,
        req ?? {},
        true,
      ),
    suspendSupplier: (supplierId: string, req?: ReasonRequest) =>
      request<void>(
        'POST',
        `/platform/suppliers/${enc(supplierId)}/suspend`,
        req ?? {},
        true,
      ),
    grantVerificationScope: (
      supplierId: string,
      scopeCode: string,
      req: GrantScopeRequest,
    ) =>
      request<VerificationStatus>(
        'POST',
        `/platform/suppliers/${enc(supplierId)}/verification/scopes/${enc(
          scopeCode,
        )}/grant`,
        req,
        true,
      ),
    suspendVerificationScope: (
      supplierId: string,
      scopeCode: string,
      req?: ReasonRequest,
    ) =>
      request<VerificationStatus>(
        'POST',
        `/platform/suppliers/${enc(supplierId)}/verification/scopes/${enc(
          scopeCode,
        )}/suspend`,
        req ?? {},
        true,
      ),
    revokeVerificationScope: (
      supplierId: string,
      scopeCode: string,
      req?: ReasonRequest,
    ) =>
      request<VerificationStatus>(
        'POST',
        `/platform/suppliers/${enc(supplierId)}/verification/scopes/${enc(
          scopeCode,
        )}/revoke`,
        req ?? {},
        true,
      ),
  };
}

export type ApiClient = ReturnType<typeof createApiClient>;
