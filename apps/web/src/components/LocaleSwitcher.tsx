'use client';

import { useTranslations } from 'next-intl';
import { useId, useTransition } from 'react';

import { usePathname, useRouter } from '@/i18n/navigation';
import {
  LOCALE_ENDONYMS,
  LOCALES,
  isSupportedLocale,
  type SupportedLocale,
} from '@/i18n/locales';

import styles from './LocaleSwitcher.module.css';

interface LocaleSwitcherProps {
  currentLocale: SupportedLocale;
}

/**
 * Native <select> language chooser listing all 20 locales by endonym (each
 * option carries its own `lang`/`hreflang`, rendered in its own language,
 * design system §6.3). Switching navigates to the same route in the chosen
 * locale via next-intl's router, which persists the explicit choice in the
 * `sc_locale` cookie (ADR-019 — no Accept-Language override afterwards).
 */
export function LocaleSwitcher({ currentLocale }: LocaleSwitcherProps) {
  const t = useTranslations('navigation');
  const router = useRouter();
  const pathname = usePathname();
  const id = useId();
  const [isPending, startTransition] = useTransition();

  function switchLocale(next: string) {
    if (!isSupportedLocale(next) || next === currentLocale) {
      return;
    }
    startTransition(() => {
      // pathname is locale-stripped; router.replace re-applies the chosen
      // locale prefix and writes the sc_locale cookie.
      router.replace(pathname, { locale: next });
    });
  }

  return (
    <span className={styles.wrapper}>
      <label className={styles.label} htmlFor={id}>
        {t('language')}
      </label>
      <select
        className={styles.select}
        id={id}
        value={currentLocale}
        disabled={isPending}
        onChange={(event) => switchLocale(event.target.value)}
      >
        {LOCALES.map((locale) => (
          <option key={locale} value={locale} lang={locale}>
            {LOCALE_ENDONYMS[locale]}
          </option>
        ))}
      </select>
    </span>
  );
}
