/**
 * Locale negotiation (ADR-019 §6). Pure, dependency-free so it is unit-testable
 * and safe to import from the Edge middleware, request config, and tests.
 *
 * Priority (highest first):
 *   1. Explicit locale in the URL path segment
 *   2. Persisted user preference — the `sc_locale` cookie
 *   3. `Accept-Language` on first visit
 *   4. `en` (deterministic fallback)
 *
 * Once a user explicitly selects a locale the cookie is set (by next-intl's
 * middleware) and thereafter wins over `Accept-Language`, so a later visit is
 * never re-redirected by the browser's language header.
 */
import {
  DEFAULT_LOCALE,
  isSupportedLocale,
  normalizeLocale,
  type SupportedLocale,
} from './locales';

interface LanguageRange {
  tag: string;
  quality: number;
}

/** Parse an `Accept-Language` header into quality-ordered ranges (highest first). */
export function parseAcceptLanguage(header: string): LanguageRange[] {
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

/** Best supported locale from an `Accept-Language` header, or null when none match. */
export function matchAcceptLanguage(header: string | null | undefined): SupportedLocale | null {
  if (!header) {
    return null;
  }
  for (const range of parseAcceptLanguage(header)) {
    if (range.tag === '*') {
      return DEFAULT_LOCALE;
    }
    // normalizeLocale never returns null; only accept it when the tag actually
    // maps to something other than the default via a real match, otherwise keep
    // scanning lower-priority ranges before conceding to the default.
    if (isSupportedLocale(range.tag)) {
      return range.tag;
    }
    const normalized = normalizeLocale(range.tag);
    const base = range.tag.toLowerCase().split('-')[0] ?? '';
    if (normalized !== DEFAULT_LOCALE || base === 'en') {
      return normalized;
    }
  }
  return null;
}

export interface NegotiationInput {
  /** Locale taken from the URL path segment, when present and valid. */
  pathLocale?: string | null | undefined;
  /** Value of the persisted `sc_locale` cookie, when present. */
  cookieLocale?: string | null | undefined;
  /** Raw `Accept-Language` request header. */
  acceptLanguage?: string | null | undefined;
}

/**
 * Resolve the active locale from the ADR-019 priority chain. Deterministic:
 * always returns a supported locale (never throws, never 500s).
 */
export function negotiateLocale(input: NegotiationInput): SupportedLocale {
  const { pathLocale, cookieLocale, acceptLanguage } = input;

  if (pathLocale && isSupportedLocale(pathLocale)) {
    return pathLocale;
  }
  if (cookieLocale && isSupportedLocale(cookieLocale)) {
    return cookieLocale;
  }
  const fromHeader = matchAcceptLanguage(acceptLanguage);
  if (fromHeader) {
    return fromHeader;
  }
  return DEFAULT_LOCALE;
}
