import type { Metadata } from 'next';
import Link from 'next/link';
import { redirect } from 'next/navigation';
import { getTranslations, setRequestLocale } from 'next-intl/server';

import { DEFAULT_LOCALE, isSupportedLocale, type SupportedLocale } from '@/i18n/locales';
import { getSession } from '@/lib/session';

import styles from './page.module.css';

interface BuyerDashboardPageProps {
  params: Promise<{ locale: string }>;
}

export async function generateMetadata({
  params,
}: BuyerDashboardPageProps): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'common' });
  return { title: t('buyer.metaTitle') };
}

export default async function BuyerDashboardPage({ params }: BuyerDashboardPageProps) {
  const { locale: raw } = await params;
  const locale: SupportedLocale = isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
  setRequestLocale(locale);

  const session = await getSession();
  if (!session) {
    // The layout already guards; this keeps the page safe if it is ever
    // moved or rendered outside that layout.
    redirect(
      `/${locale}/auth/login?returnTo=${encodeURIComponent(`/${locale}/buyer`)}`,
    );
  }

  const t = await getTranslations({ locale, namespace: 'common' });

  return (
    <section className={styles.dashboard}>
      <h1 className={styles.title}>
        {t('buyer.welcome', { name: session.user.displayName })}
      </h1>
      <p className={styles.intro}>{t('buyer.dashboardIntro')}</p>
      <div className={styles.card}>
        <h2 className={styles.cardTitle}>{t('buyer.organizationsCard.title')}</h2>
        <p className={styles.intro}>{t('buyer.organizationsCard.body')}</p>
        <Link className={styles.cardLink} href={`/${locale}/organizations`}>
          {t('buyer.organizationsCard.cta')}
        </Link>
      </div>
    </section>
  );
}
