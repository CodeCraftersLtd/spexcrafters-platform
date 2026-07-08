'use client';

import { useParams } from 'next/navigation';

import { defaultLocale, getDictionary, isLocale, type Locale } from '@/lib/i18n';

import styles from './error-pages.module.css';

interface ErrorPageProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function LocaleError({ reset }: ErrorPageProps) {
  const params = useParams<{ locale?: string }>();
  const locale: Locale =
    params?.locale && isLocale(params.locale) ? params.locale : defaultLocale;
  const copy = getDictionary(locale).errors.unexpected;

  return (
    <main className={styles.main}>
      <h1 className={styles.title}>{copy.title}</h1>
      <p className={styles.body}>{copy.body}</p>
      <button className={styles.retryButton} type="button" onClick={() => reset()}>
        {copy.retry}
      </button>
    </main>
  );
}
