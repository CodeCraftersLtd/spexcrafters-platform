import { describe, expect, it } from 'vitest';

import { ApiProblemError } from '@spexcrafters/api-client';

import { reviewerAccessFromError, reviewerAccessFromStatus } from './access';

function problem(status: number): ApiProblemError {
  return new ApiProblemError({ type: 'about:blank', title: 'x', status });
}

describe('reviewerAccessFromError', () => {
  it('maps 401 to unauthenticated', () => {
    expect(reviewerAccessFromError(problem(401))).toBe('unauthenticated');
  });

  it('maps 403 to forbidden (non-staff, e.g. an org OWNER)', () => {
    expect(reviewerAccessFromError(problem(403))).toBe('forbidden');
  });

  it('maps 404 to forbidden (existence concealment for non-staff)', () => {
    expect(reviewerAccessFromError(problem(404))).toBe('forbidden');
  });

  it('maps other API problems to error', () => {
    expect(reviewerAccessFromError(problem(500))).toBe('error');
  });

  it('maps a non-API error to error', () => {
    expect(reviewerAccessFromError(new Error('network'))).toBe('error');
  });
});

describe('reviewerAccessFromStatus', () => {
  it('maps BFF mutation statuses to reviewer states', () => {
    expect(reviewerAccessFromStatus(401)).toBe('unauthenticated');
    expect(reviewerAccessFromStatus(403)).toBe('forbidden');
    expect(reviewerAccessFromStatus(404)).toBe('forbidden');
    expect(reviewerAccessFromStatus(409)).toBe('error');
  });
});
