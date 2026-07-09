import { describe, expect, it } from 'vitest';

import { ApiProblemError } from '@spexcrafters/api-client';

import { taxonomyAccessFromError, taxonomyAccessFromStatus } from './access';

function problem(status: number): ApiProblemError {
  return new ApiProblemError({ type: 'about:blank', title: 'x', status });
}

describe('taxonomyAccessFromError', () => {
  it('maps 401 to unauthenticated', () => {
    expect(taxonomyAccessFromError(problem(401))).toBe('unauthenticated');
  });

  it('maps 403 to forbidden (non-staff caller)', () => {
    expect(taxonomyAccessFromError(problem(403))).toBe('forbidden');
  });

  it('maps 404 to forbidden (existence concealment)', () => {
    expect(taxonomyAccessFromError(problem(404))).toBe('forbidden');
  });

  it('maps other API problems to error', () => {
    expect(taxonomyAccessFromError(problem(500))).toBe('error');
  });

  it('maps a non-API error to error', () => {
    expect(taxonomyAccessFromError(new Error('network'))).toBe('error');
  });
});

describe('taxonomyAccessFromStatus', () => {
  it('maps BFF mutation statuses to taxonomy states', () => {
    expect(taxonomyAccessFromStatus(401)).toBe('unauthenticated');
    expect(taxonomyAccessFromStatus(403)).toBe('forbidden');
    expect(taxonomyAccessFromStatus(404)).toBe('forbidden');
    expect(taxonomyAccessFromStatus(409)).toBe('error');
  });
});
