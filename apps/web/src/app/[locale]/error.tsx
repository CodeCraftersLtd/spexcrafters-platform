'use client';

import { useTranslations } from 'next-intl';

import styles from './error-pages.module.css';

interface ErrorPageProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function LocaleError({ reset }: ErrorPageProps) {
  const t = useTranslations('errors.unexpected');

  return (
    <main className={styles.main}>
      <h1 className={styles.title}>{t('title')}</h1>
      <p className={styles.body}>{t('body')}</p>
      <button className={styles.retryButton} type="button" onClick={() => reset()}>
        {t('retry')}
      </button>
    </main>
  );
}
