import { ApiProblemError } from '@spexcrafters/api-client';

/**
 * Taxonomy-admin access resolution (framework-agnostic; unit-tested). The
 * /platform/taxonomy administration surface is authorized by platform-staff
 * taxonomy capabilities, NEVER by an org role — a non-staff caller gets 403
 * from every admin API and must see a forbidden state, not a generic error.
 * 401 means the session lapsed; 404 on an admin resource is existence
 * concealment (treated as forbidden); anything else is an unexpected failure.
 */
export type TaxonomyAccessState = 'ok' | 'unauthenticated' | 'forbidden' | 'error';

/** Map an API error thrown while loading a taxonomy admin resource to its UI state. */
export function taxonomyAccessFromError(error: unknown): TaxonomyAccessState {
  if (error instanceof ApiProblemError) {
    if (error.problem.status === 401) {
      return 'unauthenticated';
    }
    if (error.problem.status === 403) {
      return 'forbidden';
    }
    if (error.problem.status === 404) {
      return 'forbidden';
    }
  }
  return 'error';
}

/** Map a BFF client-error status (from a mutation response) to a taxonomy state. */
export function taxonomyAccessFromStatus(status: number): TaxonomyAccessState {
  if (status === 401) {
    return 'unauthenticated';
  }
  if (status === 403 || status === 404) {
    return 'forbidden';
  }
  return 'error';
}
