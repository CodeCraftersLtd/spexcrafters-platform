import createMiddleware from 'next-intl/middleware';
import { NextResponse, type NextRequest } from 'next/server';

import { LOCALE_ALIASES, isSupportedLocale } from '@/i18n/locales';
import { routing } from '@/i18n/routing';
import { SESSION_COOKIE_NAME } from '@/lib/cookies';

/**
 * Composed middleware (ADR-019): next-intl owns locale routing and negotiation
 * (URL → sc_locale cookie → Accept-Language → en, persisting explicit choices);
 * the Phase-1..6 session guard is preserved on top of it.
 *
 * Order matters — locale is resolved first, then the auth guard runs against the
 * already-localized path. CSRF/session integrity is a BFF-route concern (the
 * /api/* handlers) and is unaffected here.
 */
const handleI18n = createMiddleware(routing);

/** Second path segments (after the locale) that require a session cookie. */
const GUARDED_SEGMENTS = new Set([
  'buyer',
  'organizations',
  'invitations',
  'supplier',
  'reviewer',
]);

export function middleware(request: NextRequest): NextResponse {
  const { pathname, search } = request.nextUrl;
  const segments = pathname.split('/');
  const firstSegment = segments[1] ?? '';
  const secondSegment = segments[2] ?? '';

  // 1. Documented alias redirect at the edge: /zh or /zh-Hans → /zh-CN,
  //    preserving the remainder of the path and query (keeps live links working).
  const alias = LOCALE_ALIASES[firstSegment.toLowerCase()];
  if (alias && firstSegment !== alias) {
    const url = request.nextUrl.clone();
    segments[1] = alias;
    url.pathname = segments.join('/');
    return NextResponse.redirect(url);
  }

  // 2. UX auth guard for signed-in areas. Presence-only check — layouts/pages
  //    enforce the session server-side; this short-circuits the round trip for
  //    signed-out visitors. Runs only on already-localized paths so the redirect
  //    target keeps the active locale (locale-first, per ADR-019).
  if (
    isSupportedLocale(firstSegment) &&
    GUARDED_SEGMENTS.has(secondSegment) &&
    !request.cookies.has(SESSION_COOKIE_NAME)
  ) {
    const url = request.nextUrl.clone();
    url.pathname = `/${firstSegment}/auth/login`;
    url.search = '';
    url.searchParams.set('returnTo', `${pathname}${search}`);
    return NextResponse.redirect(url);
  }

  // 3. Locale negotiation / routing (adds the prefix on unprefixed requests and
  //    sets the sc_locale cookie on explicit selection).
  return handleI18n(request);
}

export const config = {
  // Skip API routes, Next internals, and any path with a file extension.
  matcher: ['/((?!api|_next|_vercel|.*\\..*).*)'],
};
