import type {
  LoginRequest,
  Problem,
  RefreshRequest,
  RegisterRequest,
  RegisterResponse,
  ResendVerificationRequest,
  TokenResponse,
  UserSummary,
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
    method: 'GET' | 'POST',
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
    const response = await fetchImpl(`${baseUrl}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      cache: 'no-store',
    });
    if (!response.ok) throw new ApiProblemError(await parseProblem(response));
    if (response.status === 204 || response.status === 202) return undefined as T;
    return (await response.json()) as T;
  }

  return {
    register: (req: RegisterRequest) => request<RegisterResponse>('POST', '/auth/register', req),
    verifyEmail: (req: VerifyEmailRequest) => request<void>('POST', '/auth/verify-email', req),
    resendVerification: (req: ResendVerificationRequest) =>
      request<void>('POST', '/auth/resend-verification', req),
    login: (req: LoginRequest) => request<TokenResponse>('POST', '/auth/login', req),
    refreshTokens: (req: RefreshRequest) => request<TokenResponse>('POST', '/auth/refresh', req),
    logout: (req: RefreshRequest) => request<void>('POST', '/auth/logout', req),
    getCurrentUser: () => request<UserSummary>('GET', '/me', undefined, true),
  };
}

export type ApiClient = ReturnType<typeof createApiClient>;
