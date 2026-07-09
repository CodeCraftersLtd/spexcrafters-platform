import Link from 'next/link';
import { getTranslations, setRequestLocale } from 'next-intl/server';
import type { ReactNode } from 'react';

import { LocaleSwitcher } from '@/components/LocaleSwitcher';
import { SkipLink } from '@/components/SkipLink';
import { DEFAULT_LOCALE, isSupportedLocale, type SupportedLocale } from '@/i18n/locales';

import styles from './layout.module.css';

interface PublicLayoutProps {
  children: ReactNode;
  params: Promise<{ locale: string }>;
}

export default async function PublicLayout({ children, params }: PublicLayoutProps) {
  const { locale: raw } = await params;
  const locale: SupportedLocale = isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
  setRequestLocale(locale);

  const [common, nav, a11y] = await Promise.all([
    getTranslations({ locale, namespace: 'common' }),
    getTranslations({ locale, namespace: 'navigation' }),
    getTranslations({ locale, namespace: 'accessibility' }),
  ]);

  // Legal pages ship in a later sprint; the localized not-found page handles
  // these routes until then, so plain anchors are used (they are not yet
  // statically typed routes).
  const legalLinks = [
    { href: `/${locale}/legal/terms`, label: nav('footer.terms') },
    { href: `/${locale}/legal/privacy`, label: nav('footer.privacy') },
    { href: `/${locale}/legal/cookies`, label: nav('footer.cookies') },
    { href: `/${locale}/accessibility`, label: nav('footer.accessibility') },
    { href: `/${locale}/imprint`, label: nav('footer.imprint') },
  ];

  return (
    <div className={styles.page}>
      <SkipLink label={a11y('skipToContent')} />
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <Link className={styles.wordmark} href={`/${locale}`}>
            {common('appName')}
          </Link>
          <nav className={styles.nav} aria-label={nav('main')} />
          <LocaleSwitcher currentLocale={locale} />
        </div>
      </header>
      <main id="main-content" className={styles.main}>
        {children}
      </main>
      <footer className={styles.footer}>
        <div className={styles.footerInner}>
          <nav aria-label={nav('legal')}>
            <ul className={styles.footerLinks}>
              {legalLinks.map((link) => (
                <li key={link.href}>
                  <a href={link.href}>{link.label}</a>
                </li>
              ))}
            </ul>
          </nav>
          <p className={styles.copyright}>
            {common('copyright', { year: new Date().getFullYear() })}
          </p>
        </div>
      </footer>
    </div>
  );
}
