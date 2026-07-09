import { describe, expect, it } from 'vitest';

import { NAMESPACES, deepMerge, loadMessages, type MessageTree } from './messages';

describe('deepMerge — deterministic en fallback', () => {
  it('overlays overrides while keeping base-only keys (fallback)', () => {
    const base: MessageTree = {
      a: 'en-a',
      nested: { x: 'en-x', y: 'en-y' },
    };
    const override: MessageTree = {
      nested: { x: 'de-x' },
    };
    expect(deepMerge(base, override)).toEqual({
      a: 'en-a',
      nested: { x: 'de-x', y: 'en-y' },
    });
  });

  it('does not mutate the base tree', () => {
    const base: MessageTree = { nested: { x: 'en-x' } };
    deepMerge(base, { nested: { x: 'de-x' } });
    expect(base).toEqual({ nested: { x: 'en-x' } });
  });
});

describe('loadMessages', () => {
  it('loads all namespaces for en', async () => {
    const messages = await loadMessages('en');
    for (const ns of NAMESPACES) {
      expect(messages[ns]).toBeTypeOf('object');
    }
    expect((messages.common as { appName: string }).appName).toBe('SpexCrafters');
  });

  it('applies real translations and falls back to en for untranslated namespaces', async () => {
    const de = await loadMessages('de');
    // navigation is a Tier-1 translated namespace for German.
    expect((de.navigation as { language: string }).language).toBe('Sprache');
    // auth is a structural placeholder → deterministic en fallback.
    const auth = de.auth as { login: { title: string } };
    expect(auth.login.title).toBe('Sign in');
  });
});
