import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { csrfFetch, sendJson } from './csrf-client';

/**
 * csrfFetch/sendJson read the JS-readable sc_csrf cookie and attach it as the
 * X-CSRF-Token header. These run in the default jsdom environment where
 * document.cookie is writable.
 */

function clearCookies(): void {
  for (const part of document.cookie.split('; ')) {
    const name = part.split('=')[0];
    if (name) {
      document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
    }
  }
}

let fetchSpy: ReturnType<typeof vi.fn>;

beforeEach(() => {
  clearCookies();
  fetchSpy = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
  vi.stubGlobal('fetch', fetchSpy);
});

afterEach(() => {
  clearCookies();
  vi.unstubAllGlobals();
});

function lastInit(): RequestInit {
  return fetchSpy.mock.calls.at(-1)?.[1] as RequestInit;
}

describe('csrfFetch', () => {
  it('attaches X-CSRF-Token from the sc_csrf cookie', async () => {
    document.cookie = 'sc_csrf=token-abc-123; path=/';
    await csrfFetch('/api/orgs', { method: 'POST' });

    const headers = new Headers(lastInit().headers);
    expect(headers.get('x-csrf-token')).toBe('token-abc-123');
    expect(lastInit().credentials).toBe('same-origin');
  });

  it('omits the header when the cookie is absent', async () => {
    await csrfFetch('/api/orgs', { method: 'POST' });
    const headers = new Headers(lastInit().headers);
    expect(headers.has('x-csrf-token')).toBe(false);
  });

  it('preserves existing headers such as content-type', async () => {
    document.cookie = 'sc_csrf=tok; path=/';
    await csrfFetch('/api/orgs', {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
    });
    const headers = new Headers(lastInit().headers);
    expect(headers.get('content-type')).toBe('application/json');
    expect(headers.get('x-csrf-token')).toBe('tok');
  });

  it('reads only sc_csrf, never a session credential', async () => {
    document.cookie = 'sc_csrf=only-this; path=/';
    // A stray readable cookie must not be picked up as the CSRF token.
    document.cookie = 'sc_other=nope; path=/';
    await csrfFetch('/api/orgs', { method: 'POST' });
    expect(new Headers(lastInit().headers).get('x-csrf-token')).toBe('only-this');
  });
});

describe('sendJson', () => {
  it('sets JSON content type and serializes the body', async () => {
    document.cookie = 'sc_csrf=tok; path=/';
    await sendJson('/api/orgs', 'POST', { name: 'Acme' });

    const init = lastInit();
    const headers = new Headers(init.headers);
    expect(init.method).toBe('POST');
    expect(headers.get('content-type')).toBe('application/json');
    expect(headers.get('x-csrf-token')).toBe('tok');
    expect(init.body).toBe(JSON.stringify({ name: 'Acme' }));
  });

  it('still sends JSON content type for a bodyless request (guard needs it)', async () => {
    document.cookie = 'sc_csrf=tok; path=/';
    await sendJson('/api/orgs/1/invitations/2/revoke', 'POST');

    const init = lastInit();
    expect(new Headers(init.headers).get('content-type')).toBe('application/json');
    expect(init.body).toBeUndefined();
  });

  it('never touches localStorage', async () => {
    const getItem = vi.spyOn(Storage.prototype, 'getItem');
    const setItem = vi.spyOn(Storage.prototype, 'setItem');
    document.cookie = 'sc_csrf=tok; path=/';
    await sendJson('/api/orgs', 'POST', { name: 'Acme' });
    expect(getItem).not.toHaveBeenCalled();
    expect(setItem).not.toHaveBeenCalled();
  });
});
