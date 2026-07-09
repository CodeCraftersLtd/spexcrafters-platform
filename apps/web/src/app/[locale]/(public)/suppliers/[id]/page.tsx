import type { Metadata } from 'next';
import { getTranslations, setRequestLocale } from 'next-intl/server';

import { ApiProblemError, type PublicSupplierProfile } from '@spexcrafters/api-client';

import { TechnicalText } from '@/components/TechnicalText';
import {
  DEFAULT_LOCALE,
  LOCALES,
  LOCALE_ENDONYMS,
  isSupportedLocale,
  type SupportedLocale,
} from '@/i18n/locales';
import { createPublicApiClient } from '@/lib/server-api';

import styles from './page.module.css';

interface PublicProfilePageProps {
  params: Promise<{ locale: string; id: string }>;
}

async function loadProfile(
  id: string,
  locale: SupportedLocale,
): Promise<PublicSupplierProfile | null> {
  try {
    return await createPublicApiClient().getPublicSupplierProfileFoundation(id, locale);
  } catch (error) {
    if (error instanceof ApiProblemError && error.problem.status === 404) {
      return null;
    }
    throw error;
  }
}

export async function generateMetadata({
  params,
}: PublicProfilePageProps): Promise<Metadata> {
  const { locale: raw, id } = await params;
  const locale: SupportedLocale = isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
  const t = await getTranslations({ locale, namespace: 'suppliers.public' });

  const profile = await loadProfile(id, locale).catch(() => null);
  if (!profile) {
    return { title: t('notFound.title'), robots: { index: false, follow: false } };
  }

  // Self-canonical + hreflang for every supported locale (ADR-022). The
  // foundation is served in the display locale with deterministic en fallback.
  const languages = Object.fromEntries(
    LOCALES.map((l) => [l, `/${l}/suppliers/${id}`]),
  );
  return {
    title: t('metaTitle', { name: profile.legalName }),
    description: t('metaDescription', { name: profile.legalName }),
    alternates: {
      canonical: `/${locale}/suppliers/${id}`,
      languages,
    },
    openGraph: {
      title: profile.legalName,
      locale: profile.displayLocale,
      type: 'profile',
    },
  };
}

export default async function PublicSupplierProfilePage({
  params,
}: PublicProfilePageProps) {
  const { locale: raw, id } = await params;
  const locale: SupportedLocale = isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
  setRequestLocale(locale);

  const t = await getTranslations({ locale, namespace: 'suppliers.public' });

  let profile: PublicSupplierProfile | null;
  try {
    profile = await loadProfile(id, locale);
  } catch {
    profile = null;
  }

  if (!profile) {
    return (
      <section className={styles.notFound}>
        <h1 className={styles.title}>{t('notFound.title')}</h1>
        <p>{t('notFound.body')}</p>
      </section>
    );
  }

  const displayLanguage =
    LOCALE_ENDONYMS[profile.displayLocale as SupportedLocale] ?? profile.displayLocale;

  return (
    <article className={styles.page}>
      {/* Legal name is Class-E — rendered LTR, never translated. */}
      <header className={styles.section}>
        <h1 className={styles.title}>
          <TechnicalText>{profile.legalName}</TechnicalText>
        </h1>
        {profile.tradingName ? <p className={styles.trading}>{profile.tradingName}</p> : null}
      </header>

      {profile.fallbackApplied ? (
        <p className={styles.notice}>{t('fallbackNotice', { language: displayLanguage })}</p>
      ) : null}
      {profile.stale ? <p className={styles.notice}>{t('staleNotice')}</p> : null}

      {profile.companyDescription ? (
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>{t('aboutTitle')}</h2>
          <p>{profile.companyDescription}</p>
        </section>
      ) : null}

      {profile.countryOfRegistration ? (
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>{t('countryLabel')}</h2>
          <p>{profile.countryOfRegistration}</p>
        </section>
      ) : null}
    </article>
  );
}
