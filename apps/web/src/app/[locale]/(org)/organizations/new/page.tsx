import type { Metadata } from 'next';

import { CreateOrganizationForm } from '@/features/organizations/CreateOrganizationForm';
import { defaultLocale, getDictionary, isLocale, type Locale } from '@/lib/i18n';

import styles from './page.module.css';

interface NewOrganizationPageProps {
  params: Promise<{ locale: string }>;
}

export async function generateMetadata({
  params,
}: NewOrganizationPageProps): Promise<Metadata> {
  const { locale } = await params;
  return { title: getDictionary(locale).organizations.create.metaTitle };
}

export default async function NewOrganizationPage({
  params,
}: NewOrganizationPageProps) {
  const { locale: raw } = await params;
  const locale: Locale = isLocale(raw) ? raw : defaultLocale;
  const dict = getDictionary(locale);
  const copy = dict.organizations.create;

  return (
    <section className={styles.page}>
      <div>
        <h1 className={styles.title}>{copy.title}</h1>
        <p className={styles.subtitle}>{copy.subtitle}</p>
      </div>
      <CreateOrganizationForm
        locale={locale}
        copy={copy}
        types={dict.organizations.types}
        validation={dict.organizations.validation}
        serverErrors={dict.organizations.serverErrors}
      />
    </section>
  );
}
