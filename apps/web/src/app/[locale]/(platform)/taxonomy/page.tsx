import type { Metadata } from 'next';
import { redirect } from 'next/navigation';
import { NextIntlClientProvider } from 'next-intl';
import { getTranslations, setRequestLocale } from 'next-intl/server';

import type {
  AttributeSummary,
  BrandSummary,
  CategoryTreeNode,
  Certification,
  EnumerationSummary,
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
import { flattenCategoryTree } from '@/features/taxonomy/tree';
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

  let tree: CategoryTreeNode[];
  let attributes: AttributeSummary[];
  let enumerations: EnumerationSummary[];
  let brands: BrandSummary[];
  let certifications: Certification[];
  let units: Unit[];
  try {
    // listAdminBrands is authenticated + platform-staff-only: it both returns brands of every
    // approval status (so PENDING brands can be reviewed/approved) AND acts as the read-time
    // staff gate — a non-staff caller gets a 403 here, which renders the forbidden state below.
    // The other reads are public (active content only).
    [tree, attributes, enumerations, brands, certifications, units] = await Promise.all([
      api.getCategoryTree(locale),
      api.listAttributes(locale),
      api.listEnumerations(locale),
      api.listAdminBrands({ locale }),
      api.listCertifications(locale),
      api.listUnits(locale),
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

  // Category admin endpoints (translations, specification template) key on the
  // entity uuid, which the tree read does not carry. Resolve code → uuid via the
  // per-category detail read so the client can target the right resource.
  const flat = flattenCategoryTree(tree);
  const resolved = await Promise.all(
    flat.map((category) =>
      api
        .getCategory(category.code, locale)
        .then((detail) => [category.code, detail.id] as const)
        .catch(() => null),
    ),
  );
  const idByCode: Record<string, string> = {};
  for (const entry of resolved) {
    if (entry) {
      idByCode[entry[0]] = entry[1];
    }
  }
  const templateCategories: TemplateCategoryOption[] = flat
    .filter((category) => idByCode[category.code])
    .map((category) => ({
      id: idByCode[category.code]!,
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
        <CategoryTree tree={tree} idByCode={idByCode} locale={locale} />
        <SpecificationTemplateForm categories={templateCategories} attributes={attributes} />
        <AttributeList attributes={attributes} locale={locale} />
        <EnumerationList enumerations={enumerations} />
        <BrandList brands={brands} locale={locale} />
        <CertificationsSection certifications={certifications} locale={locale} />
        <UnitsSection units={units} locale={locale} />
      </NextIntlClientProvider>
    </div>
  );
}
