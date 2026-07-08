import type { Metadata } from 'next';

import { VerifyEmail } from '@/features/auth/VerifyEmail';
import { defaultLocale, getDictionary, isLocale, type Locale } from '@/lib/i18n';

import styles from '../auth-page.module.css';

interface VerifyEmailPageProps {
  params: Promise<{ locale: string }>;
  searchParams: Promise<{ token?: string | string[] }>;
}

export async function generateMetadata({
  params,
}: VerifyEmailPageProps): Promise<Metadata> {
  const { locale } = await params;
  return { title: getDictionary(locale).auth.verifyEmail.metaTitle };
}

export default async function VerifyEmailPage({
  params,
  searchParams,
}: VerifyEmailPageProps) {
  const [{ locale: raw }, { token }] = await Promise.all([params, searchParams]);
  const locale: Locale = isLocale(raw) ? raw : defaultLocale;
  const dict = getDictionary(locale);
  const copy = dict.auth.verifyEmail;
  const tokenValue = Array.isArray(token) ? token[0] : token;

  return (
    <>
      <h1 className={styles.title}>{copy.title}</h1>
      <VerifyEmail
        locale={locale}
        token={tokenValue && tokenValue.length > 0 ? tokenValue : null}
        copy={copy}
        validation={dict.auth.validation}
        serverErrors={dict.auth.serverErrors}
      />
    </>
  );
}
