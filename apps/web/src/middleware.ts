import { NextResponse, type NextRequest } from 'next/server';

import { SESSION_COOKIE_NAME } from '@/lib/cookies';
import { defaultLocale, isLocale, locales, type Locale } from '@/lib/i18n';

interface LanguageRange {
  tag: string;
  quality: number;
}

function parseAcceptLanguage(header: string): LanguageRange[] {
  return header
    .split(',')
    .map((part): LanguageRange | null => {
      const [rawTag, ...params] = part.trim().split(';');
      const tag = rawTag?.trim();
      if (!tag) {
        return null;
      }
      let quality = 1;
      for (const param of params) {
        const [key, value] = param.trim().split('=');
        if (key === 'q' && value !== undefined) {
          const parsed = Number.parseFloat(value);
          if (!Number.isNaN(parsed)) {
            quality = parsed;
          }
        }
      }
      return { tag, quality };
    })
    .filter((range): range is LanguageRange => range !== null && range.quality > 0)
    .sort((a, b) => b.quality - a.quality);
}

function matchLocale(tag: string): Locale | null {
  const lower = tag.toLowerCase();
  // Exact match first (case-insensitive), e.g. "zh-hans" → "zh-Hans".
  for (const locale of locales) {
    if (locale.toLowerCase() === lower) {
      return locale;
    }
  }
  // Base-language match: "en-GB" → "en", "zh-CN"/"zh" → "zh-Hans", "fr-CA" → "fr".
  const base = lower.split('-')[0];
  if (base === 'zh') {
    return 'zh-Hans';
  }
  for (const locale of locales) {
    if (locale.toLowerCase().split('-')[0] === base) {
      return locale;
    }
  }
  return null;
}

function negotiateLocale(request: NextRequest): Locale {
  const header = request.headers.get('accept-language');
  if (!header) {
    return defaultLocale;
  }
  for (const range of parseAcceptLanguage(header)) {
    if (range.tag === '*') {
      return defaultLocale;
    }
    const match = matchLocale(range.tag);
    if (match) {
      return match;
    }
  }
  return defaultLocale;
}

/** Second path segments (after the locale) that require a session cookie. */
const GUARDED_SEGMENTS = new Set(['buyer', 'organizations', 'invitations']);

export function middleware(request: NextRequest): NextResponse {
  const { pathname, search } = request.nextUrl;
  const [, firstSegment = '', secondSegment = ''] = pathname.split('/');

  // No valid locale prefix: redirect to the negotiated locale, keeping path + query.
  if (!isLocale(firstSegment)) {
    const locale = negotiateLocale(request);
    const url = request.nextUrl.clone();
    url.pathname = `/${locale}${pathname === '/' ? '' : pathname}`;
    return NextResponse.redirect(url);
  }

  // UX auth guard for the signed-in areas. Presence-only check — the layouts
  // and pages enforce the session server-side; this just short-circuits the
  // round trip for signed-out visitors.
  if (GUARDED_SEGMENTS.has(secondSegment) && !request.cookies.has(SESSION_COOKIE_NAME)) {
    const url = request.nextUrl.clone();
    url.pathname = `/${firstSegment}/auth/login`;
    url.search = '';
    url.searchParams.set('returnTo', `${pathname}${search}`);
    return NextResponse.redirect(url);
  }

  return NextResponse.next();
}

export const config = {
  // Skip API routes, Next internals, and any path with a file extension.
  matcher: ['/((?!api|_next|.*\\..*).*)'],
};
