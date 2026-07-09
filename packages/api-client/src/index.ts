import type {
  AcceptInvitationRequest,
  ActivationRequest,
  AddEnumerationValueRequest,
  AddFacilityRequest,
  AttributeDetail,
  AttributeSummary,
  BrandApprovalRequest,
  BrandApprovalStatus,
  BrandDetail,
  BrandSummary,
  CategoryDetail,
  CategoryTreeNode,
  Certification,
  ChangeRoleRequest,
  Country,
  CreateAttributeRequest,
  CreateBrandRequest,
  CreateCategoryRequest,
  CreateCertificationRequest,
  CreateEnumerationRequest,
  CreateInvitationRequest,
  CreateOrganizationRequest,
  CreateSupplierApplicationRequest,
  CreateUnitRequest,
  DeprecationRequest,
  EffectiveSpecificationTemplate,
  EnumerationDetail,
  EnumerationSummary,
  EnumerationValueView,
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
  PutSpecificationTemplateRequest,
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
  SpecificationValidationRequest,
  SpecificationValidationResult,
  SupplierApplication,
  SupplierProfile,
  TokenResponse,
  TranslationUpsertRequest,
  TranslationView,
  Unit,
  UpdateAttributeRequest,
  UpdateCategoryRequest,
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

    // -----------------------------------------------------------------------
    // Optical taxonomy & specification registry (Phase 8) — public reads, no
    // bearer; /platform/taxonomy/** administration is platform-staff, bearer.
    // -----------------------------------------------------------------------
    getCategoryTree: (locale?: string) =>
      request<CategoryTreeNode[]>(
        'GET',
        `/taxonomy/categories${locale ? `?locale=${enc(locale)}` : ''}`,
      ),
    getCategory: (code: string, locale?: string) =>
      request<CategoryDetail>(
        'GET',
        `/taxonomy/categories/${enc(code)}${locale ? `?locale=${enc(locale)}` : ''}`,
      ),
    getCategorySpecificationTemplate: (code: string, locale?: string) =>
      request<EffectiveSpecificationTemplate>(
        'GET',
        `/taxonomy/categories/${enc(code)}/specification-template${
          locale ? `?locale=${enc(locale)}` : ''
        }`,
      ),
    listAttributes: (locale?: string) =>
      request<AttributeSummary[]>(
        'GET',
        `/taxonomy/attributes${locale ? `?locale=${enc(locale)}` : ''}`,
      ),
    getAttribute: (code: string, locale?: string) =>
      request<AttributeDetail>(
        'GET',
        `/taxonomy/attributes/${enc(code)}${locale ? `?locale=${enc(locale)}` : ''}`,
      ),
    listEnumerations: (locale?: string) =>
      request<EnumerationSummary[]>(
        'GET',
        `/taxonomy/enumerations${locale ? `?locale=${enc(locale)}` : ''}`,
      ),
    getEnumeration: (code: string, locale?: string) =>
      request<EnumerationDetail>(
        'GET',
        `/taxonomy/enumerations/${enc(code)}${locale ? `?locale=${enc(locale)}` : ''}`,
      ),
    listUnits: (locale?: string) =>
      request<Unit[]>('GET', `/taxonomy/units${locale ? `?locale=${enc(locale)}` : ''}`),
    listCountries: (locale?: string) =>
      request<Country[]>(
        'GET',
        `/taxonomy/countries${locale ? `?locale=${enc(locale)}` : ''}`,
      ),
    listCertifications: (locale?: string) =>
      request<Certification[]>(
        'GET',
        `/taxonomy/certifications${locale ? `?locale=${enc(locale)}` : ''}`,
      ),
    listBrands: (params?: { locale?: string; status?: BrandApprovalStatus }) => {
      const query = new URLSearchParams();
      if (params?.locale) query.set('locale', params.locale);
      if (params?.status) query.set('status', params.status);
      const qs = query.toString();
      return request<BrandSummary[]>('GET', `/taxonomy/brands${qs ? `?${qs}` : ''}`);
    },
    getBrand: (code: string, locale?: string) =>
      request<BrandDetail>(
        'GET',
        `/taxonomy/brands/${enc(code)}${locale ? `?locale=${enc(locale)}` : ''}`,
      ),
    /** Platform-staff: all brands in every approval status (review/approval + admin gate). */
    listAdminBrands: (params?: { locale?: string }) =>
      request<BrandSummary[]>(
        'GET',
        `/platform/taxonomy/brands${params?.locale ? `?locale=${enc(params.locale)}` : ''}`,
        undefined,
        true,
      ),
    /** Platform-staff: ALL categories (incl. inactive), flat, each with uuid + parentCode. */
    listAdminCategories: (params?: { locale?: string }) =>
      request<CategoryDetail[]>(
        'GET',
        `/platform/taxonomy/categories${params?.locale ? `?locale=${enc(params.locale)}` : ''}`,
        undefined,
        true,
      ),
    /** Platform-staff: a category's translations in every locale and status. */
    listCategoryTranslations: (id: string) =>
      request<TranslationView[]>(
        'GET',
        `/platform/taxonomy/categories/${enc(id)}/translations`,
        undefined,
        true,
      ),
    /** Platform-staff: effective specification template for a category, keyed by uuid. */
    getAdminCategorySpecificationTemplate: (id: string, locale?: string) =>
      request<EffectiveSpecificationTemplate>(
        'GET',
        `/platform/taxonomy/categories/${enc(id)}/specification-template${
          locale ? `?locale=${enc(locale)}` : ''
        }`,
        undefined,
        true,
      ),
    /** Platform-staff: ALL attributes (incl. deprecated + non-visible), as full details. */
    listAdminAttributes: (params?: { locale?: string }) =>
      request<AttributeDetail[]>(
        'GET',
        `/platform/taxonomy/attributes${params?.locale ? `?locale=${enc(params.locale)}` : ''}`,
        undefined,
        true,
      ),
    /** Platform-staff: ALL enumerations (incl. inactive), each with uuid + all values. */
    listAdminEnumerations: (params?: { locale?: string }) =>
      request<EnumerationDetail[]>(
        'GET',
        `/platform/taxonomy/enumerations${params?.locale ? `?locale=${enc(params.locale)}` : ''}`,
        undefined,
        true,
      ),
    /** Platform-staff: ALL certifications (incl. deprecated/inactive). */
    listAdminCertifications: (params?: { locale?: string }) =>
      request<Certification[]>(
        'GET',
        `/platform/taxonomy/certifications${params?.locale ? `?locale=${enc(params.locale)}` : ''}`,
        undefined,
        true,
      ),
    /** Platform-staff: ALL units (incl. inactive). */
    listAdminUnits: (params?: { locale?: string }) =>
      request<Unit[]>(
        'GET',
        `/platform/taxonomy/units${params?.locale ? `?locale=${enc(params.locale)}` : ''}`,
        undefined,
        true,
      ),
    /** Platform-staff: ALL countries (incl. inactive). */
    listAdminCountries: (params?: { locale?: string }) =>
      request<Country[]>(
        'GET',
        `/platform/taxonomy/countries${params?.locale ? `?locale=${enc(params.locale)}` : ''}`,
        undefined,
        true,
      ),
    validateSpecification: (req: SpecificationValidationRequest, locale?: string) =>
      request<SpecificationValidationResult>(
        'POST',
        `/taxonomy/specifications/validate${locale ? `?locale=${enc(locale)}` : ''}`,
        req,
      ),

    // Admin: categories.
    createCategory: (req: CreateCategoryRequest) =>
      request<CategoryDetail>('POST', '/platform/taxonomy/categories', req, true),
    updateCategory: (id: string, req: UpdateCategoryRequest) =>
      request<CategoryDetail>(
        'PATCH',
        `/platform/taxonomy/categories/${enc(id)}`,
        req,
        true,
      ),
    setCategoryActivation: (id: string, req: ActivationRequest) =>
      request<CategoryDetail>(
        'POST',
        `/platform/taxonomy/categories/${enc(id)}/activation`,
        req,
        true,
      ),
    upsertCategoryTranslation: (id: string, locale: string, req: TranslationUpsertRequest) =>
      request<TranslationView>(
        'PUT',
        `/platform/taxonomy/categories/${enc(id)}/translations/${enc(locale)}`,
        req,
        true,
      ),
    approveCategoryTranslation: (id: string, locale: string) =>
      request<TranslationView>(
        'POST',
        `/platform/taxonomy/categories/${enc(id)}/translations/${enc(locale)}/approve`,
        undefined,
        true,
      ),
    putSpecificationTemplate: (id: string, req: PutSpecificationTemplateRequest) =>
      request<EffectiveSpecificationTemplate>(
        'PUT',
        `/platform/taxonomy/categories/${enc(id)}/specification-template`,
        req,
        true,
      ),

    // Admin: attributes.
    createAttribute: (req: CreateAttributeRequest) =>
      request<AttributeDetail>('POST', '/platform/taxonomy/attributes', req, true),
    updateAttribute: (id: string, req: UpdateAttributeRequest) =>
      request<AttributeDetail>(
        'PATCH',
        `/platform/taxonomy/attributes/${enc(id)}`,
        req,
        true,
      ),
    setAttributeDeprecation: (id: string, req: DeprecationRequest) =>
      request<AttributeDetail>(
        'POST',
        `/platform/taxonomy/attributes/${enc(id)}/deprecation`,
        req,
        true,
      ),
    upsertAttributeTranslation: (id: string, locale: string, req: TranslationUpsertRequest) =>
      request<TranslationView>(
        'PUT',
        `/platform/taxonomy/attributes/${enc(id)}/translations/${enc(locale)}`,
        req,
        true,
      ),

    // Admin: enumerations.
    createEnumeration: (req: CreateEnumerationRequest) =>
      request<EnumerationDetail>('POST', '/platform/taxonomy/enumerations', req, true),
    addEnumerationValue: (id: string, req: AddEnumerationValueRequest) =>
      request<EnumerationValueView>(
        'POST',
        `/platform/taxonomy/enumerations/${enc(id)}/values`,
        req,
        true,
      ),
    upsertEnumerationValueTranslation: (
      id: string,
      locale: string,
      req: TranslationUpsertRequest,
    ) =>
      request<TranslationView>(
        'PUT',
        `/platform/taxonomy/enumeration-values/${enc(id)}/translations/${enc(locale)}`,
        req,
        true,
      ),

    // Admin: brands.
    createBrand: (req: CreateBrandRequest) =>
      request<BrandDetail>('POST', '/platform/taxonomy/brands', req, true),
    setBrandApproval: (id: string, req: BrandApprovalRequest) =>
      request<BrandDetail>(
        'POST',
        `/platform/taxonomy/brands/${enc(id)}/approval`,
        req,
        true,
      ),
    upsertBrandTranslation: (id: string, locale: string, req: TranslationUpsertRequest) =>
      request<TranslationView>(
        'PUT',
        `/platform/taxonomy/brands/${enc(id)}/translations/${enc(locale)}`,
        req,
        true,
      ),

    // Admin: certifications & units.
    createCertification: (req: CreateCertificationRequest) =>
      request<Certification>('POST', '/platform/taxonomy/certifications', req, true),
    createUnit: (req: CreateUnitRequest) =>
      request<Unit>('POST', '/platform/taxonomy/units', req, true),
  };
}

export type ApiClient = ReturnType<typeof createApiClient>;
