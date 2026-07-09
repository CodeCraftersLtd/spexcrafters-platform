import type { Metadata } from 'next';
import { redirect } from 'next/navigation';
import { NextIntlClientProvider } from 'next-intl';
import { getTranslations, setRequestLocale } from 'next-intl/server';

import type { LocaleInfo, MyMembership } from '@spexcrafters/api-client';

import {
  CreateApplicationForm,
  type LocaleOption,
  type OrgOption,
} from '@/features/suppliers/CreateApplicationForm';
import {
  DEFAULT_LOCALE,
  LOCALE_ENDONYMS,
  isSupportedLocale,
  type SupportedLocale,
} from '@/i18n/locales';
import { pickMessages } from '@/i18n/pick';
import { createServerApiClient } from '@/lib/server-api';
import { getSession } from '@/lib/session';

import styles from './page.module.css';

interface SupplierHomePageProps {
  params: Promise<{ locale: string }>;
}

export async function generateMetadata({
  params,
}: SupplierHomePageProps): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'suppliers.home' });
  return { title: t('metaTitle') };
}

export default async function SupplierHomePage({ params }: SupplierHomePageProps) {
  const { locale: raw } = await params;
  const locale: SupportedLocale = isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
  setRequestLocale(locale);

  const session = await getSession();
  if (!session) {
    redirect(`/${locale}/auth/login?returnTo=${encodeURIComponent(`/${locale}/supplier`)}`);
  }

  const t = await getTranslations({ locale, namespace: 'suppliers.home' });
  const api = createServerApiClient(session.accessToken);

  const [memberships, locales] = await Promise.all([
    api.listMyOrganizations().catch((): MyMembership[] => []),
    api.listLocales().catch((): LocaleInfo[] => []),
  ]);

  const organizations: OrgOption[] = memberships.map((membership) => ({
    id: membership.organizationId,
    name: membership.organizationName,
  }));

  const localeOptions: LocaleOption[] = (
    locales.length > 0 ? locales.map((info) => info.code) : [...Object.keys(LOCALE_ENDONYMS)]
  ).map((code) => ({
    code,
    label: LOCALE_ENDONYMS[code as SupportedLocale] ?? code,
  }));

  const messages = await pickMessages(['suppliers', 'errors', 'common', 'accessibility']);

  return (
    <section className={styles.page}>
      <div>
        <h1 className={styles.title}>{t('title')}</h1>
        <p className={styles.intro}>{t('intro')}</p>
      </div>
      <NextIntlClientProvider locale={locale} messages={messages}>
        <CreateApplicationForm
          locale={locale}
          organizations={organizations}
          locales={localeOptions}
        />
      </NextIntlClientProvider>
    </section>
  );
}
