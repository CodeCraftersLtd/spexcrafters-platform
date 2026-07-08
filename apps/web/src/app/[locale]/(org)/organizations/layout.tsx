import type { Metadata } from 'next';
import Link from 'next/link';
import { redirect } from 'next/navigation';
import type { ReactNode } from 'react';

import { LocaleSwitcher } from '@/components/LocaleSwitcher';
import { SkipLink } from '@/components/SkipLink';
import { LogoutButton } from '@/features/auth/LogoutButton';
import { defaultLocale, getDictionary, isLocale, type Locale } from '@/lib/i18n';
import { getSession } from '@/lib/session';

import styles from './layout.module.css';

export const metadata: Metadata = {
  robots: { index: false, follow: false },
};

interface OrganizationsLayoutProps {
  children: ReactNode;
  params: Promise<{ locale: string }>;
}

export default async function OrganizationsLayout({
  children,
  params,
}: OrganizationsLayoutProps) {
  const { locale: raw } = await params;
  const locale: Locale = isLocale(raw) ? raw : defaultLocale;
  const dict = getDictionary(locale);

  // Server-side enforcement — the middleware cookie check is UX only.
  const session = await getSession();
  if (!session) {
    redirect(
      `/${locale}/auth/login?returnTo=${encodeURIComponent(`/${locale}/organizations`)}`,
    );
  }

  return (
    <div className={styles.shell}>
      <SkipLink label={dict.common.skipToContent} />
      <aside className={styles.sidebar}>
        <Link className={styles.wordmark} href={`/${locale}`}>
          {dict.common.appName}
        </Link>
        <nav aria-label={dict.organizations.navigationLabel}>
          <ul className={styles.navList}>
            <li>
              <Link className={styles.navLink} href={`/${locale}/buyer`}>
                {dict.organizations.nav.dashboard}
              </Link>
            </li>
            <li>
              <Link className={styles.navLink} href={`/${locale}/organizations`}>
                {dict.organizations.nav.organizations}
              </Link>
            </li>
          </ul>
        </nav>
      </aside>
      <div className={styles.content}>
        <header className={styles.topbar}>
          <LocaleSwitcher
            currentLocale={locale}
            label={dict.common.localeSwitcherLabel}
          />
          <LogoutButton locale={locale} label={dict.organizations.logout} />
        </header>
        <main id="main-content" className={styles.main}>
          {children}
        </main>
      </div>
    </div>
  );
}
