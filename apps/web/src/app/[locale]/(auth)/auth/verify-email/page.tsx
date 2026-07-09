import type { Metadata } from 'next';
import { NextIntlClientProvider } from 'next-intl';
import { getTranslations, setRequestLocale } from 'next-intl/server';

import { VerifyEmail } from '@/features/auth/VerifyEmail';
import { DEFAULT_LOCALE, isSupportedLocale, type SupportedLocale } from '@/i18n/locales';
import { pickMessages } from '@/i18n/pick';

import styles from '../auth-page.module.css';

interface VerifyEmailPageProps {
  params: Promise<{ locale: string }>;
  searchParams: Promise<{ token?: string | string[] }>;
}

export async function generateMetadata({
  params,
}: VerifyEmailPageProps): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'auth' });
  return { title: t('verifyEmail.metaTitle') };
}

export default async function VerifyEmailPage({
  params,
  searchParams,
}: VerifyEmailPageProps) {
  const [{ locale: raw }, { token }] = await Promise.all([params, searchParams]);
  const locale: SupportedLocale = isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
  setRequestLocale(locale);

  const t = await getTranslations({ locale, namespace: 'auth.verifyEmail' });
  const messages = await pickMessages(['auth', 'common']);
  const tokenValue = Array.isArray(token) ? token[0] : token;

  return (
    <>
      <h1 className={styles.title}>{t('title')}</h1>
      <NextIntlClientProvider locale={locale} messages={messages}>
        <VerifyEmail
          locale={locale}
          token={tokenValue && tokenValue.length > 0 ? tokenValue : null}
        />
      </NextIntlClientProvider>
    </>
  );
}
