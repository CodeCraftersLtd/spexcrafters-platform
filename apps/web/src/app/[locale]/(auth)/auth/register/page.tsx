import type { Metadata } from 'next';

import { RegisterForm } from '@/features/auth/RegisterForm';
import { defaultLocale, getDictionary, isLocale, type Locale } from '@/lib/i18n';

import styles from '../auth-page.module.css';

interface RegisterPageProps {
  params: Promise<{ locale: string }>;
}

export async function generateMetadata({ params }: RegisterPageProps): Promise<Metadata> {
  const { locale } = await params;
  return { title: getDictionary(locale).auth.register.metaTitle };
}

export default async function RegisterPage({ params }: RegisterPageProps) {
  const { locale: raw } = await params;
  const locale: Locale = isLocale(raw) ? raw : defaultLocale;
  const dict = getDictionary(locale);
  const copy = dict.auth.register;

  return (
    <>
      <h1 className={styles.title}>{copy.title}</h1>
      <p className={styles.subtitle}>{copy.subtitle}</p>
      <RegisterForm
        locale={locale}
        copy={copy}
        validation={dict.auth.validation}
        serverErrors={dict.auth.serverErrors}
      />
    </>
  );
}
