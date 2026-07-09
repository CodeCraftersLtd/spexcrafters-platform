/**
 * Structural shape of a next-intl namespace translator (`useTranslations(ns)` /
 * `getTranslations(ns)`). Used at the boundary of framework-agnostic helpers
 * (zod schema builders, error mappers) so they depend on this minimal contract
 * rather than on next-intl's strictly-typed key unions. Pass a next-intl `t`
 * via a thin `(key) => t(key)` adapter (see the feature components); tests can
 * supply a plain object literal.
 */
export interface Translator {
  (key: string, values?: Record<string, string | number | Date>): string;
  has(key: string): boolean;
}

/** Translator without the membership probe — for message sets read key-by-key. */
export type TranslateFn = (
  key: string,
  values?: Record<string, string | number | Date>,
) => string;
