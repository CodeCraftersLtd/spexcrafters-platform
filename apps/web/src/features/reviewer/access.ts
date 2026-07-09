import { ApiProblemError } from '@spexcrafters/api-client';

/**
 * Reviewer access resolution (framework-agnostic; unit-tested). The reviewer
 * surface is authorized by platform capabilities, NEVER by an org role — a
 * non-staff caller (even an org OWNER) gets 403 from every reviewer API and
 * must see a forbidden state, not a generic error. 401 means the session
 * lapsed; anything else is an unexpected failure.
 */
export type ReviewerAccessState = 'ok' | 'unauthenticated' | 'forbidden' | 'error';

/** Map an API error thrown while loading a reviewer resource to its UI state. */
export function reviewerAccessFromError(error: unknown): ReviewerAccessState {
  if (error instanceof ApiProblemError) {
    if (error.problem.status === 401) {
      return 'unauthenticated';
    }
    if (error.problem.status === 403) {
      return 'forbidden';
    }
    // 404 on a reviewer resource is existence concealment for non-staff — treat
    // it as forbidden so we never confirm an application id to an outsider.
    if (error.problem.status === 404) {
      return 'forbidden';
    }
  }
  return 'error';
}

/** Map a BFF client-error status (from a mutation response) to a reviewer state. */
export function reviewerAccessFromStatus(status: number): ReviewerAccessState {
  if (status === 401) {
    return 'unauthenticated';
  }
  if (status === 403 || status === 404) {
    return 'forbidden';
  }
  return 'error';
}
