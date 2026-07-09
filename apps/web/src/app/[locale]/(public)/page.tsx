import type { Metadata } from 'next';
import Link from 'next/link';
import { getTranslations, setRequestLocale } from 'next-intl/server';

import { Button } from '@spexcrafters/ui';

import { DEFAULT_LOCALE, isSupportedLocale, type SupportedLocale } from '@/i18n/locales';

import styles from './page.module.css';

interface HomePageProps {
  params: Promise<{ locale: string }>;
}

export async function generateMetadata({ params }: HomePageProps): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'seo' });
  return {
    title: t('home.title'),
    description: t('home.description'),
  };
}

export default async function HomePage({ params }: HomePageProps) {
  const { locale: raw } = await params;
  const locale: SupportedLocale = isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
  setRequestLocale(locale);

  const t = await getTranslations({ locale, namespace: 'common' });

  return (
    <section className={styles.hero}>
      <div className={styles.heroInner}>
        <span className={styles.heroKeyline} aria-hidden="true" />
        <h1 className={styles.heroTitle}>{t('home.hero.title')}</h1>
        <p className={styles.heroSubtitle}>{t('home.hero.subtitle')}</p>
        <div className={styles.ctaRow}>
          <Link className={styles.primaryCta} href={`/${locale}/auth/register`}>
            {t('home.cta.createAccount')}
          </Link>
          <Button variant="secondary" size="md" type="button" disabled>
            {t('home.cta.findSuppliers')}
            <span className="sc-visually-hidden"> ({t('comingSoon')})</span>
          </Button>
        </div>
      </div>
    </section>
  );
}
