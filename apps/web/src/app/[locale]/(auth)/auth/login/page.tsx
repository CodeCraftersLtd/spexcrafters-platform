import type { Metadata } from 'next';

import { LoginForm } from '@/features/auth/LoginForm';
import { defaultLocale, getDictionary, isLocale, type Locale } from '@/lib/i18n';

import styles from '../auth-page.module.css';

interface LoginPageProps {
  params: Promise<{ locale: string }>;
  searchParams: Promise<{ returnTo?: string | string[] }>;
}

export async function generateMetadata({ params }: LoginPageProps): Promise<Metadata> {
  const { locale } = await params;
  return { title: getDictionary(locale).auth.login.metaTitle };
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
  const locale: Locale = isLocale(raw) ? raw : defaultLocale;
  const dict = getDictionary(locale);
  const copy = dict.auth.login;

  return (
    <>
      <h1 className={styles.title}>{copy.title}</h1>
      <p className={styles.subtitle}>{copy.subtitle}</p>
      <LoginForm
        locale={locale}
        returnTo={sanitizeReturnTo(returnTo)}
        copy={copy}
        validation={dict.auth.validation}
        serverErrors={dict.auth.serverErrors}
      />
    </>
  );
}
