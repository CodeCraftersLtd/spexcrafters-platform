import type { Metadata } from 'next';
import { redirect } from 'next/navigation';

import {
  defaultLocale,
  getDictionary,
  interpolate,
  isLocale,
  type Locale,
} from '@/lib/i18n';
import { getSession } from '@/lib/session';

import styles from './page.module.css';

interface BuyerDashboardPageProps {
  params: Promise<{ locale: string }>;
}

export async function generateMetadata({
  params,
}: BuyerDashboardPageProps): Promise<Metadata> {
  const { locale } = await params;
  return { title: getDictionary(locale).buyer.metaTitle };
}

export default async function BuyerDashboardPage({ params }: BuyerDashboardPageProps) {
  const { locale: raw } = await params;
  const locale: Locale = isLocale(raw) ? raw : defaultLocale;
  const dict = getDictionary(locale);

  const session = await getSession();
  if (!session) {
    // The layout already guards; this keeps the page safe if it is ever
    // moved or rendered outside that layout.
    redirect(
      `/${locale}/auth/login?returnTo=${encodeURIComponent(`/${locale}/buyer`)}`,
    );
  }

  return (
    <section className={styles.dashboard}>
      <h1 className={styles.title}>
        {interpolate(dict.buyer.welcome, { name: session.user.displayName })}
      </h1>
      <p className={styles.intro}>{dict.buyer.dashboardIntro}</p>
    </section>
  );
}
