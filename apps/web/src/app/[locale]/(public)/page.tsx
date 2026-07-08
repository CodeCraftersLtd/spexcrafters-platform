import type { Metadata } from 'next';
import Link from 'next/link';

import { Button } from '@spexcrafters/ui';

import { defaultLocale, getDictionary, isLocale, type Locale } from '@/lib/i18n';

import styles from './page.module.css';

interface HomePageProps {
  params: Promise<{ locale: string }>;
}

export async function generateMetadata({ params }: HomePageProps): Promise<Metadata> {
  const { locale } = await params;
  const dict = getDictionary(locale);
  return {
    title: dict.home.metaTitle,
    description: dict.home.metaDescription,
  };
}

export default async function HomePage({ params }: HomePageProps) {
  const { locale: raw } = await params;
  const locale: Locale = isLocale(raw) ? raw : defaultLocale;
  const dict = getDictionary(locale);

  return (
    <section className={styles.hero}>
      <div className={styles.heroInner}>
        <span className={styles.heroKeyline} aria-hidden="true" />
        <h1 className={styles.heroTitle}>{dict.home.hero.title}</h1>
        <p className={styles.heroSubtitle}>{dict.home.hero.subtitle}</p>
        <div className={styles.ctaRow}>
          <Link className={styles.primaryCta} href={`/${locale}/auth/register`}>
            {dict.home.cta.createAccount}
          </Link>
          <Button variant="secondary" size="md" type="button" disabled>
            {dict.home.cta.findSuppliers}
            <span className="sc-visually-hidden"> ({dict.common.comingSoon})</span>
          </Button>
        </div>
      </div>
    </section>
  );
}
