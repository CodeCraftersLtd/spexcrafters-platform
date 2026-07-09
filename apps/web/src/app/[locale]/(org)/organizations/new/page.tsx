import type { Metadata } from 'next';
import { NextIntlClientProvider } from 'next-intl';
import { getTranslations, setRequestLocale } from 'next-intl/server';

import { CreateOrganizationForm } from '@/features/organizations/CreateOrganizationForm';
import { DEFAULT_LOCALE, isSupportedLocale, type SupportedLocale } from '@/i18n/locales';
import { pickMessages } from '@/i18n/pick';

import styles from './page.module.css';

interface NewOrganizationPageProps {
  params: Promise<{ locale: string }>;
}

export async function generateMetadata({
  params,
}: NewOrganizationPageProps): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'organizations.create' });
  return { title: t('metaTitle') };
}

export default async function NewOrganizationPage({
  params,
}: NewOrganizationPageProps) {
  const { locale: raw } = await params;
  const locale: SupportedLocale = isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
  setRequestLocale(locale);

  const t = await getTranslations({ locale, namespace: 'organizations.create' });
  const messages = await pickMessages(['organizations', 'common']);

  return (
    <section className={styles.page}>
      <div>
        <h1 className={styles.title}>{t('title')}</h1>
        <p className={styles.subtitle}>{t('subtitle')}</p>
      </div>
      <NextIntlClientProvider locale={locale} messages={messages}>
        <CreateOrganizationForm locale={locale} />
      </NextIntlClientProvider>
    </section>
  );
}
