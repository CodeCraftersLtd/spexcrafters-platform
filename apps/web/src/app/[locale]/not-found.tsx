'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';

import { DEFAULT_LOCALE, isSupportedLocale, type SupportedLocale } from '@/i18n/locales';

import styles from './error-pages.module.css';

export default function NotFound() {
  const t = useTranslations('errors.notFound');
  const params = useParams<{ locale?: string }>();
  const locale: SupportedLocale =
    params?.locale && isSupportedLocale(params.locale) ? params.locale : DEFAULT_LOCALE;

  return (
    <main className={styles.main}>
      <p className={styles.code} aria-hidden="true">
        404
      </p>
      <h1 className={styles.title}>{t('title')}</h1>
      <p className={styles.body}>{t('body')}</p>
      <Link href={`/${locale}`}>{t('backHome')}</Link>
    </main>
  );
}
