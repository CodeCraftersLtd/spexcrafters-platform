import de from '../../messages/de.json';
import en from '../../messages/en.json';
import fr from '../../messages/fr.json';
import zhHans from '../../messages/zh-Hans.json';

export const locales = ['en', 'zh-Hans', 'fr', 'de'] as const;
export type Locale = (typeof locales)[number];
export const defaultLocale: Locale = 'en';

/** The English dictionary is the schema; every locale file must satisfy it. */
export type Dictionary = typeof en;

const dictionaries: Record<Locale, Dictionary> = {
  en,
  'zh-Hans': zhHans,
  fr,
  de,
};

export function isLocale(value: string): value is Locale {
  return (locales as readonly string[]).includes(value);
}

/**
 * Resolve the dictionary for a locale. Unknown values fall back to the
 * default locale so a stale or hand-edited URL can never crash rendering
 * (the middleware normally guarantees a valid locale segment).
 */
export function getDictionary(locale: string): Dictionary {
  return isLocale(locale) ? dictionaries[locale] : dictionaries[defaultLocale];
}

/**
 * Native-language display names for the locale switcher. Proper nouns are
 * rendered in their own language by design (LocaleSwitcher a11y spec), so
 * they live here rather than in per-locale dictionaries.
 */
export const localeDisplayNames: Record<Locale, string> = {
  en: 'English',
  'zh-Hans': '简体中文',
  fr: 'Français',
  de: 'Deutsch',
};

/** Replace `{name}`-style placeholders in a dictionary message. */
export function interpolate(
  template: string,
  values: Record<string, string | number>,
): string {
  return template.replace(/\{(\w+)\}/g, (match, key: string) => {
    const value = values[key];
    return value === undefined ? match : String(value);
  });
}
