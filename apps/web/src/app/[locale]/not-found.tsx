'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';

import { defaultLocale, getDictionary, isLocale, type Locale } from '@/lib/i18n';

import styles from './error-pages.module.css';

export default function NotFound() {
  const params = useParams<{ locale?: string }>();
  const locale: Locale =
    params?.locale && isLocale(params.locale) ? params.locale : defaultLocale;
  const copy = getDictionary(locale).errors.notFound;

  return (
    <main className={styles.main}>
      <p className={styles.code} aria-hidden="true">
        404
      </p>
      <h1 className={styles.title}>{copy.title}</h1>
      <p className={styles.body}>{copy.body}</p>
      <Link href={`/${locale}`}>{copy.backHome}</Link>
    </main>
  );
}
