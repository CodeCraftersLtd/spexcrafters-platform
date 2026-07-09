import { createNavigation } from 'next-intl/navigation';

import { routing } from './routing';

/**
 * Locale-aware navigation primitives. `Link`/`useRouter`/`redirect` keep the
 * active locale prefix automatically; `useRouter().replace(pathname, { locale })`
 * both switches locale and persists the `sc_locale` cookie (used by
 * LocaleSwitcher). `usePathname()` returns the path WITHOUT the locale prefix.
 */
export const { Link, redirect, usePathname, useRouter, getPathname } =
  createNavigation(routing);
