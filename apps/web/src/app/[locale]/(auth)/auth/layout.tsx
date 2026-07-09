import type { Metadata } from 'next';
import Link from 'next/link';
import { getTranslations, setRequestLocale } from 'next-intl/server';
import type { ReactNode } from 'react';

import { SkipLink } from '@/components/SkipLink';
import { DEFAULT_LOCALE, isSupportedLocale, type SupportedLocale } from '@/i18n/locales';

import styles from './layout.module.css';

export const metadata: Metadata = {
  robots: { index: false, follow: false },
};

interface AuthLayoutProps {
  children: ReactNode;
  params: Promise<{ locale: string }>;
}

export default async function AuthLayout({ children, params }: AuthLayoutProps) {
  const { locale: raw } = await params;
  const locale: SupportedLocale = isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
  setRequestLocale(locale);

  const [common, a11y] = await Promise.all([
    getTranslations({ locale, namespace: 'common' }),
    getTranslations({ locale, namespace: 'accessibility' }),
  ]);

  return (
    <div className={styles.page}>
      <SkipLink label={a11y('skipToContent')} />
      <header className={styles.header}>
        <Link className={styles.wordmark} href={`/${locale}`}>
          {common('appName')}
        </Link>
      </header>
      <main id="main-content" className={styles.main}>
        <div className={styles.card}>{children}</div>
      </main>
    </div>
  );
}
