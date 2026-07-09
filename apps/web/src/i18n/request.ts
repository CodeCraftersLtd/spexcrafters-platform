import { cookies, headers } from 'next/headers';
import { getRequestConfig } from 'next-intl/server';

import { isSupportedLocale, type SupportedLocale } from './locales';
import { loadMessages } from './messages';
import { negotiateLocale } from './negotiation';

/**
 * next-intl request config. Resolves the active locale (URL segment first, then
 * the ADR-019 negotiation chain when a request has no locale prefix) and returns
 * the en-backed messages for it. Runs on the server only — client islands
 * receive just the namespaces they need via NextIntlClientProvider.
 */
export default getRequestConfig(async ({ requestLocale }) => {
  const requested = await requestLocale;

  let locale: SupportedLocale;
  if (requested && isSupportedLocale(requested)) {
    locale = requested;
  } else {
    // No valid locale on the segment (e.g. a non-prefixed internal render):
    // fall back through the documented priority chain deterministically.
    const [cookieStore, headerStore] = await Promise.all([cookies(), headers()]);
    locale = negotiateLocale({
      cookieLocale: cookieStore.get('sc_locale')?.value,
      acceptLanguage: headerStore.get('accept-language'),
    });
  }

  return {
    locale,
    messages: await loadMessages(locale),
  };
});
