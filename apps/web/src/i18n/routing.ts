import { defineRouting } from 'next-intl/routing';

import { DEFAULT_LOCALE, LOCALES } from './locales';

/**
 * next-intl routing config (ADR-019). Locale is always the first path segment
 * for every route, public and authenticated. The explicit choice is persisted
 * in the `sc_locale` cookie so `Accept-Language` never overrides it on a later
 * visit.
 */
export const routing = defineRouting({
  locales: LOCALES,
  defaultLocale: DEFAULT_LOCALE,
  // Every route carries its locale prefix, including the default (`/en/…`).
  localePrefix: 'always',
  // Detect from cookie → Accept-Language on unprefixed requests.
  localeDetection: true,
  localeCookie: {
    name: 'sc_locale',
    // Persisted preference: 1 year, host-only, lax (matches session cookies).
    maxAge: 60 * 60 * 24 * 365,
    sameSite: 'lax',
  },
});
