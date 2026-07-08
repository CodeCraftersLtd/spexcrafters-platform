import { createApiClient, type ApiClient } from '@spexcrafters/api-client';

const API_BASE_URL = process.env.API_BASE_URL ?? 'http://localhost:8080/api/v1';

/**
 * Typed API client bound to a bearer access token. Used by Server Components
 * for reads (with the token from getSession()) and by BFF Route Handlers for
 * mutations (with the token from refreshIfNeeded()).
 */
export function createServerApiClient(accessToken: string): ApiClient {
  return createApiClient({
    baseUrl: API_BASE_URL,
    getAccessToken: () => accessToken,
  });
}
