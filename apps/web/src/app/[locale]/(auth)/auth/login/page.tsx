import type { Metadata } from 'next';
import { NextIntlClientProvider } from 'next-intl';
import { getTranslations, setRequestLocale } from 'next-intl/server';

import { LoginForm } from '@/features/auth/LoginForm';
import { DEFAULT_LOCALE, isSupportedLocale, type SupportedLocale } from '@/i18n/locales';
import { pickMessages } from '@/i18n/pick';

import styles from '../auth-page.module.css';

interface LoginPageProps {
  params: Promise<{ locale: string }>;
  searchParams: Promise<{ returnTo?: string | string[] }>;
}

export async function generateMetadata({ params }: LoginPageProps): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'auth' });
  return { title: t('login.metaTitle') };
}

/** Only same-origin absolute paths are honored (prevents open redirects). */
function sanitizeReturnTo(value: string | string[] | undefined): string | undefined {
  const candidate = Array.isArray(value) ? value[0] : value;
  if (candidate && candidate.startsWith('/') && !candidate.startsWith('//')) {
    return candidate;
  }
  return undefined;
}

export default async function LoginPage({ params, searchParams }: LoginPageProps) {
  const [{ locale: raw }, { returnTo }] = await Promise.all([params, searchParams]);
  const locale: SupportedLocale = isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
  setRequestLocale(locale);

  const t = await getTranslations({ locale, namespace: 'auth.login' });
  const messages = await pickMessages(['auth', 'common']);

  return (
    <>
      <h1 className={styles.title}>{t('title')}</h1>
      <p className={styles.subtitle}>{t('subtitle')}</p>
      <NextIntlClientProvider locale={locale} messages={messages}>
        <LoginForm locale={locale} returnTo={sanitizeReturnTo(returnTo)} />
      </NextIntlClientProvider>
    </>
  );
}
