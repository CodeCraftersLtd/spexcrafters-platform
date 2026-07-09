import type { Metadata } from 'next';
import { redirect } from 'next/navigation';
import { NextIntlClientProvider } from 'next-intl';
import { getTranslations, setRequestLocale } from 'next-intl/server';

import { AcceptInvitation } from '@/features/organizations/AcceptInvitation';
import { DEFAULT_LOCALE, isSupportedLocale, type SupportedLocale } from '@/i18n/locales';
import { pickMessages } from '@/i18n/pick';
import { getSession } from '@/lib/session';

import styles from './page.module.css';

interface AcceptInvitationPageProps {
  params: Promise<{ locale: string }>;
  searchParams: Promise<{ token?: string | string[] }>;
}

export async function generateMetadata({
  params,
}: AcceptInvitationPageProps): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'organizations.acceptInvitation' });
  return {
    title: t('metaTitle'),
    robots: { index: false, follow: false },
  };
}

export default async function AcceptInvitationPage({
  params,
  searchParams,
}: AcceptInvitationPageProps) {
  const [{ locale: raw }, { token }] = await Promise.all([params, searchParams]);
  const locale: SupportedLocale = isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
  setRequestLocale(locale);

  const t = await getTranslations({ locale, namespace: 'organizations.acceptInvitation' });
  const messages = await pickMessages(['organizations', 'common']);
  const tokenValue = Array.isArray(token) ? token[0] : token;

  // Server-side enforcement — the middleware cookie check is UX only. The
  // returnTo keeps the single-use token so acceptance resumes after sign-in.
  const session = await getSession();
  if (!session) {
    const returnTo = `/${locale}/invitations/accept${
      tokenValue ? `?token=${encodeURIComponent(tokenValue)}` : ''
    }`;
    redirect(`/${locale}/auth/login?returnTo=${encodeURIComponent(returnTo)}`);
  }

  return (
    <div className={styles.wrapper}>
      <section className={styles.card}>
        <h1 className={styles.title}>{t('title')}</h1>
        <NextIntlClientProvider locale={locale} messages={messages}>
          <AcceptInvitation
            locale={locale}
            token={tokenValue && tokenValue.length > 0 ? tokenValue : null}
          />
        </NextIntlClientProvider>
      </section>
    </div>
  );
}
