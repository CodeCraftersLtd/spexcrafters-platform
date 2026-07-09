import { NextRequest } from 'next/server';
import { describe, expect, it, vi } from 'vitest';

// Stub next-intl's locale middleware so we can assert the composition around it
// (alias redirect + auth guard) without exercising negotiation internals.
vi.mock('next-intl/middleware', () => ({
  default: () => () =>
    new Response(null, { status: 200, headers: { 'x-i18n': 'handled' } }),
}));

import { middleware } from './middleware';

function request(path: string, cookie?: string): NextRequest {
  return new NextRequest(new URL(`http://localhost${path}`), {
    headers: cookie ? { cookie } : {},
  });
}

describe('composed middleware', () => {
  it('redirects the zh / zh-Hans alias to zh-CN, preserving the path', () => {
    const response = middleware(request('/zh-Hans/organizations'));
    expect(response.status).toBe(307);
    expect(response.headers.get('location')).toContain('/zh-CN/organizations');
  });

  it('redirects a signed-out visitor of a guarded segment to localized login', () => {
    const response = middleware(request('/en/organizations'));
    expect(response.status).toBe(307);
    const location = response.headers.get('location') ?? '';
    expect(location).toContain('/en/auth/login');
    expect(location).toContain('returnTo=');
  });

  it('lets a signed-in visitor through to the locale middleware', () => {
    const response = middleware(request('/en/organizations', 'sc_session=sealed'));
    expect(response.headers.get('x-i18n')).toBe('handled');
  });

  it('does not guard public segments', () => {
    const response = middleware(request('/en/auth/login'));
    expect(response.headers.get('x-i18n')).toBe('handled');
  });

  it('leaves the buyer guard intact', () => {
    const response = middleware(request('/de/buyer'));
    expect(response.status).toBe(307);
    expect(response.headers.get('location')).toContain('/de/auth/login');
  });
});
