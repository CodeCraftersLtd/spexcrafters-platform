'use client';

import type { Route } from 'next';
import { usePathname, useRouter } from 'next/navigation';
import { useId } from 'react';

import { isLocale, localeDisplayNames, locales, type Locale } from '@/lib/i18n';

import styles from './LocaleSwitcher.module.css';

interface LocaleSwitcherProps {
  currentLocale: Locale;
  /** Localized "Language" label. */
  label: string;
}

/**
 * Native <select> language chooser. Options are rendered in their own
 * language with a matching lang attribute (design system §6.3 LocaleSwitcher).
 */
export function LocaleSwitcher({ currentLocale, label }: LocaleSwitcherProps) {
  const router = useRouter();
  const pathname = usePathname();
  const id = useId();

  function switchLocale(next: string) {
    if (!isLocale(next) || next === currentLocale) {
      return;
    }
    const segments = pathname.split('/');
    // segments[0] is '' (leading slash); segments[1] is the locale prefix.
    if (segments.length > 1 && segments[1] !== undefined && isLocale(segments[1])) {
      segments[1] = next;
    } else {
      segments.splice(1, 0, next);
    }
    // The rebuilt path targets the same route in another locale; the cast is
    // required because a joined string cannot be statically typed as a Route.
    router.replace(segments.join('/') as Route);
  }

  return (
    <span className={styles.wrapper}>
      <label className={styles.label} htmlFor={id}>
        {label}
      </label>
      <select
        className={styles.select}
        id={id}
        value={currentLocale}
        onChange={(event) => switchLocale(event.target.value)}
      >
        {locales.map((locale) => (
          <option key={locale} value={locale} lang={locale}>
            {localeDisplayNames[locale]}
          </option>
        ))}
      </select>
    </span>
  );
}
