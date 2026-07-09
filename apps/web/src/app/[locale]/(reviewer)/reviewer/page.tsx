import type { Metadata } from 'next';
import Link from 'next/link';
import { redirect } from 'next/navigation';
import { getTranslations, setRequestLocale } from 'next-intl/server';

import type { ReviewQueuePage } from '@spexcrafters/api-client';

import { reviewerAccessFromError } from '@/features/reviewer/access';
import { DEFAULT_LOCALE, isSupportedLocale, type SupportedLocale } from '@/i18n/locales';
import { createServerApiClient } from '@/lib/server-api';
import { getSession } from '@/lib/session';

import styles from './queue.module.css';

interface ReviewerQueuePageProps {
  params: Promise<{ locale: string }>;
  searchParams: Promise<{ cursor?: string }>;
}

export async function generateMetadata({
  params,
}: ReviewerQueuePageProps): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'reviewer.queue' });
  return { title: t('metaTitle') };
}

export default async function ReviewerQueuePage({
  params,
  searchParams,
}: ReviewerQueuePageProps) {
  const { locale: raw } = await params;
  const { cursor } = await searchParams;
  const locale: SupportedLocale = isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
  setRequestLocale(locale);

  const session = await getSession();
  if (!session) {
    redirect(`/${locale}/auth/login?returnTo=${encodeURIComponent(`/${locale}/reviewer`)}`);
  }

  const [t, statusCopy, forbidden] = await Promise.all([
    getTranslations({ locale, namespace: 'reviewer.queue' }),
    getTranslations({ locale, namespace: 'suppliers.status' }),
    getTranslations({ locale, namespace: 'reviewer' }),
  ]);

  const api = createServerApiClient(session.accessToken);

  let page: ReviewQueuePage;
  try {
    page = await api.listReviewQueue(cursor ? { cursor, size: 20 } : { size: 20 });
  } catch (error) {
    const state = reviewerAccessFromError(error);
    if (state === 'unauthenticated') {
      redirect(`/${locale}/auth/login?returnTo=${encodeURIComponent(`/${locale}/reviewer`)}`);
    }
    if (state === 'forbidden') {
      return (
        <section className={styles.forbidden}>
          <h1 className={styles.title}>{forbidden('forbidden.title')}</h1>
          <p className={styles.intro}>{forbidden('forbidden.body')}</p>
        </section>
      );
    }
    return (
      <section className={styles.page}>
        <p role="alert">{forbidden('loadError')}</p>
      </section>
    );
  }

  const dateFormatter = new Intl.DateTimeFormat(locale, { dateStyle: 'medium' });

  return (
    <section className={styles.page}>
      <div>
        <h1 className={styles.title}>{t('title')}</h1>
        <p className={styles.intro}>{t('intro')}</p>
        <p className={styles.count}>{t('count', { count: page.items.length })}</p>
      </div>

      {page.items.length === 0 ? (
        <p className={styles.empty}>{t('empty')}</p>
      ) : (
        <ul className={styles.list}>
          {page.items.map((item) => {
            const label = statusCopy.has(`state.${item.status}`)
              ? statusCopy(`state.${item.status}`)
              : item.status;
            return (
              <li key={item.applicationId} className={styles.row}>
                <div>
                  <Link
                    className={styles.rowLink}
                    href={`/${locale}/reviewer/${item.applicationId}`}
                    aria-label={t('open', { name: item.legalName ?? item.applicationId })}
                  >
                    <span dir="ltr">{item.legalName ?? item.applicationId}</span>
                  </Link>
                  <p className={styles.rowMeta}>
                    {label}
                    {item.submittedAt
                      ? ` · ${t('submittedLabel', {
                          date: dateFormatter.format(new Date(item.submittedAt)),
                        })}`
                      : ''}
                    {` · ${t('originalLocaleLabel')}: ${item.originalLocale}`}
                  </p>
                </div>
              </li>
            );
          })}
        </ul>
      )}

      {page.nextCursor ? (
        <Link
          className={styles.next}
          href={`/${locale}/reviewer?cursor=${encodeURIComponent(page.nextCursor)}`}
        >
          {t('next')}
        </Link>
      ) : null}
    </section>
  );
}
