/**
 * SpexCrafters locale registry (Phase 7) — the single frontend source of truth
 * for supported locales. Mirrors docs/architecture/supported-locales.md and the
 * backend `SupportedLocale`. Codes are BCP 47.
 *
 * Adding a locale = one row here + one `messages/{locale}/` directory. No other
 * code branches on the specific locale (RTL is derived from RTL_LOCALES; the
 * script/font group lives in the typography layer).
 */

/** The 20 launch locales, in registry order. */
export const LOCALES = [
  'en',
  'zh-CN',
  'ko',
  'ja',
  'th',
  'vi',
  'fr',
  'es',
  'id',
  'ms',
  'hi',
  'ru',
  'bn',
  'de',
  'ur',
  'fil',
  'fa',
  'pt',
  'ar',
  'tr',
] as const;

export type SupportedLocale = (typeof LOCALES)[number];

/** Canonical deterministic fallback — every UI key and content fallback ends here. */
export const DEFAULT_LOCALE: SupportedLocale = 'en';

/** First-class right-to-left locales (Arabic script). Everything else is ltr. */
export const RTL_LOCALES: ReadonlySet<SupportedLocale> = new Set([
  'ar',
  'fa',
  'ur',
]);

export type Direction = 'ltr' | 'rtl';

const LOCALE_SET: ReadonlySet<string> = new Set(LOCALES);

/**
 * Documented aliases resolved at the edge (redirect) and during normalization.
 * `zh` / `zh-Hans` (the Phase 1..6 code) collapse to the registry code `zh-CN`.
 * Keys are compared case-insensitively.
 */
export const LOCALE_ALIASES: Readonly<Record<string, SupportedLocale>> = {
  zh: 'zh-CN',
  'zh-hans': 'zh-CN',
  'zh-cn': 'zh-CN',
  'zh-sg': 'zh-CN',
  in: 'id', // legacy ISO 639 code for Indonesian
};

/**
 * Native language names (endonyms) for the locale switcher. Language-independent
 * by design — "Deutsch" reads the same in every UI locale — so they live in the
 * registry (no message load needed to render the switcher) and are mirrored into
 * each locale's `common.localeName` for CI parity.
 */
export const LOCALE_ENDONYMS: Readonly<Record<SupportedLocale, string>> = {
  en: 'English',
  'zh-CN': '简体中文',
  ko: '한국어',
  ja: '日本語',
  th: 'ไทย',
  vi: 'Tiếng Việt',
  fr: 'Français',
  es: 'Español',
  id: 'Bahasa Indonesia',
  ms: 'Bahasa Melayu',
  hi: 'हिन्दी',
  ru: 'Русский',
  bn: 'বাংলা',
  de: 'Deutsch',
  ur: 'اردو',
  fil: 'Filipino',
  fa: 'فارسی',
  pt: 'Português',
  ar: 'العربية',
  tr: 'Türkçe',
};

/** True when `value` is one of the 20 registry codes (exact, canonical case). */
export function isSupportedLocale(value: string): value is SupportedLocale {
  return LOCALE_SET.has(value);
}

/**
 * Resolve any incoming language tag to a supported locale. Never throws.
 *
 * Priority: exact (case-insensitive) match → documented alias → base-language
 * match (`en-GB` → `en`, `pt-BR` → `pt`, any `zh-*` → `zh-CN`) → `en`.
 * `x-default`, unknown, and empty inputs deterministically resolve to `en`.
 */
export function normalizeLocale(input: string | null | undefined): SupportedLocale {
  if (!input) {
    return DEFAULT_LOCALE;
  }
  const trimmed = input.trim();
  if (!trimmed || trimmed === '*' || trimmed.toLowerCase() === 'x-default') {
    return DEFAULT_LOCALE;
  }

  // Exact, case-insensitive match to a canonical code.
  const lower = trimmed.toLowerCase();
  for (const locale of LOCALES) {
    if (locale.toLowerCase() === lower) {
      return locale;
    }
  }

  // Documented alias (whole-tag).
  const alias = LOCALE_ALIASES[lower];
  if (alias && isSupportedLocale(alias)) {
    return alias;
  }

  // Base-language match: strip region/script subtags.
  const base = lower.split('-')[0] ?? lower;
  if (base === 'zh') {
    return 'zh-CN';
  }
  const baseAlias = LOCALE_ALIASES[base];
  if (baseAlias && isSupportedLocale(baseAlias)) {
    return baseAlias;
  }
  for (const locale of LOCALES) {
    if ((locale.toLowerCase().split('-')[0] ?? '') === base) {
      return locale;
    }
  }

  return DEFAULT_LOCALE;
}

/** Writing direction for a locale (defensively normalizes unknown input). */
export function dirOf(locale: string): Direction {
  const normalized = isSupportedLocale(locale) ? locale : normalizeLocale(locale);
  return RTL_LOCALES.has(normalized) ? 'rtl' : 'ltr';
}
