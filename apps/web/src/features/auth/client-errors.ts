import type { Dictionary } from '@/lib/i18n';

/** Mirror of the BFF error envelope (src/lib/bff.ts), as seen by the browser. */
export interface BffFieldError {
  code: string;
  message: string;
}

export interface BffError {
  code: string;
  message: string;
  fields?: Record<string, BffFieldError>;
}

function isBffError(value: unknown): value is BffError {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const candidate = value as Record<string, unknown>;
  return typeof candidate.code === 'string' && typeof candidate.message === 'string';
}

/** Parse a non-2xx BFF response body into a BffError, tolerating garbage. */
export async function readBffError(response: Response): Promise<BffError> {
  try {
    const body = (await response.json()) as { error?: unknown };
    if (isBffError(body.error)) {
      return body.error;
    }
  } catch {
    // Fall through to the generic error.
  }
  return { code: 'unexpected', message: '' };
}

/**
 * Resolve an error code to localized copy: known codes come from the
 * dictionary, unknown codes fall back to the server-provided (already
 * locale-resolved) message, then to the generic error.
 */
export function translateError(
  error: Pick<BffError, 'code' | 'message'>,
  serverErrors: Dictionary['auth']['serverErrors'],
): string {
  const known = (serverErrors as Record<string, string>)[error.code];
  if (known) {
    return known;
  }
  if (error.message) {
    return error.message;
  }
  return serverErrors.unexpected;
}
