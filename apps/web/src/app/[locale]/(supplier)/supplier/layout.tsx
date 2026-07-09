import type { Metadata } from 'next';
import Link from 'next/link';
import { redirect } from 'next/navigation';
import { getTranslations, setRequestLocale } from 'next-intl/server';
import type { ReactNode } from 'react';

import { LocaleSwitcher } from '@/components/LocaleSwitcher';
import { SkipLink } from '@/components/SkipLink';
import { LogoutButton } from '@/features/auth/LogoutButton';
import { DEFAULT_LOCALE, isSupportedLocale, type SupportedLocale } from '@/i18n/locales';
import { getSession } from '@/lib/session';

import styles from './layout.module.css';

export const metadata: Metadata = {
  robots: { index: false, follow: false },
};

interface SupplierLayoutProps {
  children: ReactNode;
  params: Promise<{ locale: string }>;
}

export default async function SupplierLayout({
  children,
  params,
}: SupplierLayoutProps) {
  const { locale: raw } = await params;
  const locale: SupportedLocale = isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
  setRequestLocale(locale);

  // Server-side enforcement — the middleware cookie check is UX only.
  const session = await getSession();
  if (!session) {
    redirect(
      `/${locale}/auth/login?returnTo=${encodeURIComponent(`/${locale}/supplier`)}`,
    );
  }

  const [common, nav, navigation, a11y] = await Promise.all([
    getTranslations({ locale, namespace: 'common' }),
    getTranslations({ locale, namespace: 'suppliers.nav' }),
    getTranslations({ locale, namespace: 'navigation' }),
    getTranslations({ locale, namespace: 'accessibility' }),
  ]);

  return (
    <div className={styles.shell}>
      <SkipLink label={a11y('skipToContent')} />
      <aside className={styles.sidebar}>
        <Link className={styles.wordmark} href={`/${locale}`}>
          {common('appName')}
        </Link>
        <nav aria-label={nav('label')}>
          <ul className={styles.navList}>
            <li>
              <Link className={styles.navLink} href={`/${locale}/supplier`}>
                {nav('home')}
              </Link>
            </li>
            <li>
              <Link className={styles.navLink} href={`/${locale}/buyer`}>
                {nav('dashboard')}
              </Link>
            </li>
          </ul>
        </nav>
      </aside>
      <div className={styles.content}>
        <header className={styles.topbar}>
          <LocaleSwitcher currentLocale={locale} />
          <LogoutButton locale={locale} label={navigation('logout')} />
        </header>
        <main id="main-content" className={styles.main}>
          {children}
        </main>
      </div>
    </div>
  );
}
