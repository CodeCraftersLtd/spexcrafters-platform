import type { Metadata } from 'next';
import Link from 'next/link';
import type { ReactNode } from 'react';

import { SkipLink } from '@/components/SkipLink';
import { defaultLocale, getDictionary, isLocale, type Locale } from '@/lib/i18n';

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
  const locale: Locale = isLocale(raw) ? raw : defaultLocale;
  const dict = getDictionary(locale);

  return (
    <div className={styles.page}>
      <SkipLink label={dict.common.skipToContent} />
      <header className={styles.header}>
        <Link className={styles.wordmark} href={`/${locale}`}>
          {dict.common.appName}
        </Link>
      </header>
      <main id="main-content" className={styles.main}>
        <div className={styles.card}>{children}</div>
      </main>
    </div>
  );
}
