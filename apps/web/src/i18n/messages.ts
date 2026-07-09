/**
 * Message-resource loading. Messages live at `messages/{locale}/{namespace}.json`
 * (ICU MessageFormat). `en` is the canonical source; every other locale is
 * loaded on top of the `en` base and deep-merged so that any gap resolves
 * deterministically to English (Class A fallback, localization-content
 * classification §fallback).
 */
import { DEFAULT_LOCALE, type SupportedLocale } from './locales';

/** The twelve message namespaces. Order is irrelevant; CI enforces the set. */
export const NAMESPACES = [
  'common',
  'navigation',
  'auth',
  'organizations',
  'suppliers',
  'verification',
  'evidence',
  'reviewer',
  'errors',
  'accessibility',
  'seo',
  'taxonomy',
] as const;

export type Namespace = (typeof NAMESPACES)[number];

export type MessageTree = { [key: string]: string | MessageTree };
export type Messages = Record<Namespace, MessageTree>;

function isPlainObject(value: unknown): value is MessageTree {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * Deep-merge `override` onto `base`, returning a new tree. Scalar and shape
 * conflicts resolve to `override`; keys present only in `base` (the en source)
 * survive — this is the deterministic en fallback.
 */
export function deepMerge(base: MessageTree, override: MessageTree): MessageTree {
  const result: MessageTree = { ...base };
  for (const [key, value] of Object.entries(override)) {
    const existing = result[key];
    if (isPlainObject(existing) && isPlainObject(value)) {
      result[key] = deepMerge(existing, value);
    } else {
      result[key] = value;
    }
  }
  return result;
}

async function loadNamespace(
  locale: SupportedLocale,
  namespace: Namespace,
): Promise<MessageTree> {
  const mod = (await import(`../../messages/${locale}/${namespace}.json`)) as {
    default: MessageTree;
  };
  return mod.default;
}

/**
 * Load the full, en-backed message set for a locale. For `en` this is just the
 * source; for any other locale each namespace is merged over the en base so
 * missing keys fall back to English.
 */
export async function loadMessages(locale: SupportedLocale): Promise<Messages> {
  const result = {} as Messages;
  for (const namespace of NAMESPACES) {
    const base = await loadNamespace(DEFAULT_LOCALE, namespace);
    if (locale === DEFAULT_LOCALE) {
      result[namespace] = base;
    } else {
      const localized = await loadNamespace(locale, namespace);
      result[namespace] = deepMerge(base, localized);
    }
  }
  return result;
}
