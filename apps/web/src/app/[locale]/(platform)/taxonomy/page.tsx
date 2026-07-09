import type { Metadata } from 'next';
import { redirect } from 'next/navigation';
import { NextIntlClientProvider } from 'next-intl';
import { getTranslations, setRequestLocale } from 'next-intl/server';

import type {
  AttributeDetail,
  BrandSummary,
  CategoryDetail,
  Certification,
  EnumerationDetail,
  Unit,
} from '@spexcrafters/api-client';

import { taxonomyAccessFromError } from '@/features/taxonomy/access';
import { AttributeList } from '@/features/taxonomy/AttributeList';
import { BrandList } from '@/features/taxonomy/BrandList';
import { CategoryTree } from '@/features/taxonomy/CategoryTree';
import { CertificationsSection } from '@/features/taxonomy/CertificationsSection';
import { EnumerationList } from '@/features/taxonomy/EnumerationList';
import {
  SpecificationTemplateForm,
  type TemplateCategoryOption,
} from '@/features/taxonomy/SpecificationTemplateForm';
import { UnitsSection } from '@/features/taxonomy/UnitsSection';
import { DEFAULT_LOCALE, isSupportedLocale, type SupportedLocale } from '@/i18n/locales';
import { pickMessages } from '@/i18n/pick';
import { createServerApiClient } from '@/lib/server-api';
import { getSession } from '@/lib/session';

import styles from '@/features/taxonomy/taxonomy.module.css';

interface TaxonomyPageProps {
  params: Promise<{ locale: string }>;
}

export async function generateMetadata({
  params,
}: TaxonomyPageProps): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'taxonomyAdmin.dashboard' });
  return { title: t('metaTitle') };
}

export default async function TaxonomyDashboardPage({ params }: TaxonomyPageProps) {
  const { locale: raw } = await params;
  const locale: SupportedLocale = isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
  setRequestLocale(locale);

  const session = await getSession();
  if (!session) {
    redirect(`/${locale}/auth/login?returnTo=${encodeURIComponent(`/${locale}/taxonomy`)}`);
  }

  const t = await getTranslations({ locale, namespace: 'taxonomyAdmin' });
  const api = createServerApiClient(session.accessToken);

  let categories: CategoryDetail[];
  let attributes: AttributeDetail[];
  let enumerations: EnumerationDetail[];
  let brands: BrandSummary[];
  let certifications: Certification[];
  let units: Unit[];
  try {
    // Every read here is authenticated + platform-staff-only (TAXONOMY_READ): staff see ALL
    // states (inactive/deprecated/pending), not just the public active/approved surface. Any of
    // them returning 403 is the read-time staff gate — a non-staff caller lands in the forbidden
    // state below. listAdminCategories also supplies the code → uuid map in one call, replacing
    // the old per-category detail fan-out.
    [categories, attributes, enumerations, brands, certifications, units] = await Promise.all([
      api.listAdminCategories({ locale }),
      api.listAdminAttributes({ locale }),
      api.listAdminEnumerations({ locale }),
      api.listAdminBrands({ locale }),
      api.listAdminCertifications({ locale }),
      api.listAdminUnits({ locale }),
    ]);
  } catch (error) {
    const state = taxonomyAccessFromError(error);
    if (state === 'unauthenticated') {
      redirect(`/${locale}/auth/login?returnTo=${encodeURIComponent(`/${locale}/taxonomy`)}`);
    }
    if (state === 'forbidden') {
      return (
        <section className={styles.forbidden}>
          <h1 className={styles.title}>{t('forbidden.title')}</h1>
          <p className={styles.intro}>{t('forbidden.body')}</p>
        </section>
      );
    }
    return (
      <section className={styles.page}>
        <p role="alert">{t('loadError')}</p>
      </section>
    );
  }

  // The flat listAdminCategories read already carries each category's uuid, so the
  // specification-template picker options come straight from it — no per-category
  // detail fan-out. (categoryIdByCode is available if a code → uuid map is needed.)
  const templateCategories: TemplateCategoryOption[] = categories.map((category) => ({
    id: category.id,
    code: category.code,
    name: category.name,
    depth: category.depth,
  }));

  const messages = pickMessages(['taxonomyAdmin', 'errors', 'common', 'accessibility']);

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>{t('dashboard.title')}</h1>
        <p className={styles.intro}>{t('dashboard.intro')}</p>
        <p className={styles.notice}>{t('dashboard.codeNotice')}</p>
      </header>

      <NextIntlClientProvider locale={locale} messages={await messages}>
        <CategoryTree categories={categories} locale={locale} />
        <SpecificationTemplateForm categories={templateCategories} attributes={attributes} />
        <AttributeList attributes={attributes} locale={locale} />
        <EnumerationList enumerations={enumerations} locale={locale} />
        <BrandList brands={brands} locale={locale} />
        <CertificationsSection certifications={certifications} locale={locale} />
        <UnitsSection units={units} locale={locale} />
      </NextIntlClientProvider>
    </div>
  );
}
