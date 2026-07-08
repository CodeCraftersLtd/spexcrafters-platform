import Link from 'next/link';
import type { ReactNode } from 'react';

import { LocaleSwitcher } from '@/components/LocaleSwitcher';
import { SkipLink } from '@/components/SkipLink';
import {
  defaultLocale,
  getDictionary,
  interpolate,
  isLocale,
  type Locale,
} from '@/lib/i18n';

import styles from './layout.module.css';

interface PublicLayoutProps {
  children: ReactNode;
  params: Promise<{ locale: string }>;
}

export default async function PublicLayout({ children, params }: PublicLayoutProps) {
  const { locale: raw } = await params;
  const locale: Locale = isLocale(raw) ? raw : defaultLocale;
  const dict = getDictionary(locale);

  // Legal pages ship in a later sprint; the localized not-found page handles
  // these routes until then, so plain anchors are used (they are not yet
  // statically typed routes).
  const legalLinks = [
    { href: `/${locale}/legal/terms`, label: dict.common.footer.terms },
    { href: `/${locale}/legal/privacy`, label: dict.common.footer.privacy },
    { href: `/${locale}/legal/cookies`, label: dict.common.footer.cookies },
    { href: `/${locale}/accessibility`, label: dict.common.footer.accessibility },
    { href: `/${locale}/imprint`, label: dict.common.footer.imprint },
  ];

  return (
    <div className={styles.page}>
      <SkipLink label={dict.common.skipToContent} />
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <Link className={styles.wordmark} href={`/${locale}`}>
            {dict.common.appName}
          </Link>
          <nav
            className={styles.nav}
            aria-label={dict.common.mainNavigationLabel}
          />
          <LocaleSwitcher
            currentLocale={locale}
            label={dict.common.localeSwitcherLabel}
          />
        </div>
      </header>
      <main id="main-content" className={styles.main}>
        {children}
      </main>
      <footer className={styles.footer}>
        <div className={styles.footerInner}>
          <nav aria-label={dict.common.footerLegalLabel}>
            <ul className={styles.footerLinks}>
              {legalLinks.map((link) => (
                <li key={link.href}>
                  <a href={link.href}>{link.label}</a>
                </li>
              ))}
            </ul>
          </nav>
          <p className={styles.copyright}>
            {interpolate(dict.common.footer.copyright, {
              year: new Date().getFullYear(),
            })}
          </p>
        </div>
      </footer>
    </div>
  );
}
