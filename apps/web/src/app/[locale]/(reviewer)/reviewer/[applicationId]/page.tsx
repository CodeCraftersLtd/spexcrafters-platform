import type { Metadata } from 'next';
import Link from 'next/link';
import { redirect } from 'next/navigation';
import { NextIntlClientProvider } from 'next-intl';
import { getTranslations, setRequestLocale } from 'next-intl/server';
import type { ReactNode } from 'react';

import {
  type ReviewDetail,
  type VerificationStatus,
} from '@spexcrafters/api-client';

import type { Translator } from '@/i18n/translator';
import { reviewerAccessFromError } from '@/features/reviewer/access';
import { ReviewerActions } from '@/features/reviewer/ReviewerActions';
import { VerificationGrant } from '@/features/reviewer/VerificationGrant';
import { TechnicalText } from '@/components/TechnicalText';
import {
  needsStaleWarning,
  orderTranslations,
  translationIndicator,
} from '@/features/suppliers/translations';
import { taxonomyLabel } from '@/features/suppliers/taxonomy';
import {
  DEFAULT_LOCALE,
  LOCALE_ENDONYMS,
  isSupportedLocale,
  type SupportedLocale,
} from '@/i18n/locales';
import { pickMessages } from '@/i18n/pick';
import { createServerApiClient } from '@/lib/server-api';
import { getSession } from '@/lib/session';

import styles from './detail.module.css';

interface DetailPageProps {
  params: Promise<{ locale: string; applicationId: string }>;
}

export async function generateMetadata({
  params,
}: DetailPageProps): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'reviewer.detail' });
  return { title: t('metaTitle') };
}

export default async function ReviewerDetailPage({ params }: DetailPageProps) {
  const { locale: raw, applicationId } = await params;
  const locale: SupportedLocale = isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
  setRequestLocale(locale);

  const session = await getSession();
  if (!session) {
    redirect(`/${locale}/auth/login?returnTo=${encodeURIComponent(`/${locale}/reviewer`)}`);
  }

  const [r, d, s, e, taxonomy, verificationCopy] = await Promise.all([
    getTranslations({ locale, namespace: 'reviewer' }),
    getTranslations({ locale, namespace: 'reviewer.detail' }),
    getTranslations({ locale, namespace: 'suppliers' }),
    getTranslations({ locale, namespace: 'evidence' }),
    getTranslations({ locale, namespace: 'taxonomy' }),
    getTranslations({ locale, namespace: 'verification.status' }),
  ]);
  const tax = taxonomy as unknown as Translator;

  const api = createServerApiClient(session.accessToken);

  let detail: ReviewDetail;
  try {
    detail = await api.getReviewDetail(applicationId);
  } catch (error) {
    const state = reviewerAccessFromError(error);
    if (state === 'unauthenticated') {
      redirect(`/${locale}/auth/login?returnTo=${encodeURIComponent(`/${locale}/reviewer`)}`);
    }
    if (state === 'forbidden') {
      return (
        <section className={styles.forbidden}>
          <h1 className={styles.title}>{r('forbidden.title')}</h1>
          <p className={styles.intro}>{r('forbidden.body')}</p>
          <Link href={`/${locale}/reviewer`}>{d('backToQueue')}</Link>
        </section>
      );
    }
    return (
      <section className={styles.page}>
        <p role="alert">{r('loadError')}</p>
      </section>
    );
  }

  const verification = await api
    .getVerificationStatus(detail.supplierId)
    .catch((): VerificationStatus | null => null);

  const { profile } = detail;
  const original = orderTranslations(profile.translations).find((row) => row.original);
  const translations = orderTranslations(profile.translations).filter((row) => !row.original);
  const messages = await pickMessages([
    'reviewer',
    'verification',
    'taxonomy',
    'errors',
    'common',
    'accessibility',
  ]);

  const statusCopy = await getTranslations({ locale, namespace: 'suppliers.status' });
  const statusLabel = statusCopy.has(`state.${detail.status}`)
    ? statusCopy(`state.${detail.status}`)
    : detail.status;

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div>
          <Link href={`/${locale}/reviewer`}>{d('backToQueue')}</Link>
          <h1 className={styles.title}>{d('title')}</h1>
        </div>
        <span className={styles.badge}>{statusLabel}</span>
      </header>

      <Providers messages={messages} locale={locale}>
        <section className={styles.section}>
          <ReviewerActions
            applicationId={detail.applicationId}
            supplierId={detail.supplierId}
            status={detail.status}
            operationalStatus={detail.operationalStatus}
          />
        </section>
      </Providers>

      {/* Company information */}
      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>{d('companyInfo')}</h2>
        <dl className={styles.summary}>
          <div className={styles.summaryItem}>
            <dt className={styles.summaryLabel}>{s('companyInfo.legalNameLabel')}</dt>
            <dd><TechnicalText>{profile.legalName}</TechnicalText></dd>
          </div>
          {profile.registrationNumber ? (
            <div className={styles.summaryItem}>
              <dt className={styles.summaryLabel}>{s('companyInfo.registrationNumberLabel')}</dt>
              <dd><TechnicalText>{profile.registrationNumber}</TechnicalText></dd>
            </div>
          ) : null}
          {profile.countryOfRegistration ? (
            <div className={styles.summaryItem}>
              <dt className={styles.summaryLabel}>{s('companyInfo.countryLabel')}</dt>
              <dd>{profile.countryOfRegistration}</dd>
            </div>
          ) : null}
          {profile.companyTypeCode ? (
            <div className={styles.summaryItem}>
              <dt className={styles.summaryLabel}>{s('companyInfo.companyTypeLabel')}</dt>
              <dd>{taxonomyLabel(tax, 'companyType', profile.companyTypeCode)}</dd>
            </div>
          ) : null}
        </dl>
      </section>

      {/* Optical profile */}
      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>{d('opticalProfile')}</h2>
        <p className={styles.rowMeta}>{s('opticalProfile.typesLabel')}</p>
        <p>{profile.types.map((code) => taxonomyLabel(tax, 'supplierType', code)).join(', ') || '—'}</p>
        <p className={styles.rowMeta}>{s('opticalProfile.capabilitiesLabel')}</p>
        <p>{profile.capabilities.map((c) => taxonomyLabel(tax, 'capability', c.capabilityCode)).join(', ') || '—'}</p>
      </section>

      {/* Original content + translations */}
      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>{d('originalContent')}</h2>
        <p className={styles.rowMeta}>
          {d('originalLocaleLabel')}: {LOCALE_ENDONYMS[original?.locale as SupportedLocale] ?? original?.locale ?? '—'}
        </p>
        {original?.companyDescription ? <p>{original.companyDescription}</p> : <p>—</p>}

        {translations.length > 0 ? (
          <>
            <h3 className={styles.subTitle}>{d('translations')}</h3>
            <p className={styles.rowMeta}>{r('fallbackNotice')}</p>
            <ul className={styles.list}>
              {translations.map((row) => (
                <li key={row.locale} className={styles.translationRow}>
                  <span className={styles.badge}>
                    {LOCALE_ENDONYMS[row.locale as SupportedLocale] ?? row.locale}
                  </span>
                  <span className={styles.badge}>{r(`translations.indicator.${translationIndicator(row)}`)}</span>
                  {needsStaleWarning(row) ? (
                    <span className={styles.stale}>{r('translations.staleWarning')}</span>
                  ) : null}
                  {row.companyDescription ? <p>{row.companyDescription}</p> : null}
                </li>
              ))}
            </ul>
          </>
        ) : null}
      </section>

      {/* Evidence */}
      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>{d('evidence')}</h2>
        {detail.evidence.some((item) => item.scanStatus !== 'CLEAN') ? (
          <p className={styles.stale}>{e('scanBanner')}</p>
        ) : null}
        {detail.evidence.length === 0 ? (
          <p className={styles.rowMeta}>{d('noEvidence')}</p>
        ) : (
          <ul className={styles.list}>
            {detail.evidence.map((item) => (
              <li key={item.id} className={styles.evidenceRow}>
                <span dir="ltr">{item.originalFilename}</span>
                <span className={styles.rowMeta}>
                  {taxonomyLabel(tax, 'evidenceType', item.evidenceTypeCode)} ·{' '}
                  {e('scanLabel')}: {e(`scan.${item.scanStatus}`)}
                </span>
                {item.downloadable ? (
                  <a
                    className={styles.badge}
                    href={`/api/supplier/${encodeURIComponent(detail.supplierId)}/evidence/${encodeURIComponent(item.id)}/download`}
                  >
                    {e('download')}
                  </a>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* Change requests */}
      {detail.changeRequests.length > 0 ? (
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>{d('changeRequests')}</h2>
          <ul className={styles.list}>
            {detail.changeRequests.map((cr) => (
              <li key={cr.id} className={styles.translationRow}>
                <strong>{cr.requestedItem}</strong>
                <span className={styles.badge}>{s(`changeRequests.state.${cr.status}`)}</span>
                <p>{cr.reason}</p>
                {cr.supplierResponse ? <p className={styles.rowMeta}>{cr.supplierResponse}</p> : null}
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      {/* Verification (scope grant + suspend/revoke) */}
      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>{d('verification')}</h2>
        <p className={styles.rowMeta}>{verificationCopy('disclaimer')}</p>
        <Providers messages={messages} locale={locale}>
          <VerificationGrant
            supplierId={detail.supplierId}
            evidence={detail.evidence}
            verification={verification}
          />
        </Providers>
      </section>
    </div>
  );
}

function Providers({
  messages,
  locale,
  children,
}: {
  messages: Record<string, unknown>;
  locale: SupportedLocale;
  children: React.ReactNode;
}) {
  return (
    <NextIntlClientProvider locale={locale} messages={messages}>
      {children}
    </NextIntlClientProvider>
  );
}
