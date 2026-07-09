'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type {
  BrandApprovalRequest,
  BrandSummary,
  CreateBrandRequest,
} from '@spexcrafters/api-client';
import { Alert, Button, FormField } from '@spexcrafters/ui';

import type { TranslateFn, Translator } from '@/i18n/translator';
import { LOCALE_ENDONYMS, LOCALES } from '@/i18n/locales';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';

import { BRAND_TYPES } from './constants';
import { translateTaxonomyError } from './errors';
import { createBrandSchema, type CreateBrandValues } from './schemas';
import styles from './taxonomy.module.css';

interface BrandListProps {
  brands: BrandSummary[];
  locale: string;
}

export function BrandList({ brands, locale }: BrandListProps) {
  const t = useTranslations('taxonomyAdmin.brands');
  const statusCopy = useTranslations('taxonomyAdmin.brandApprovalStatus') as unknown as Translator;
  const typeCopy = useTranslations('taxonomyAdmin.brandType') as unknown as Translator;

  return (
    <div className={styles.section} id="brands">
      <h2 className={styles.sectionTitle}>{t('title')}</h2>
      <p className={styles.intro}>{t('intro')}</p>

      {brands.length === 0 ? (
        <p className={styles.empty}>{t('empty')}</p>
      ) : (
        <ul className={styles.list}>
          {brands.map((brand) => (
            <li key={brand.code} className={styles.row} data-brand-code={brand.code}>
              <div className={styles.rowMain}>
                <span className={styles.rowName}>{brand.displayName ?? brand.canonicalName}</span>
                <span className={styles.rowMeta}>
                  <span className={styles.code}>{brand.code}</span>
                  {' · '}
                  {typeCopy.has(brand.brandType) ? typeCopy(brand.brandType) : brand.brandType}
                </span>
              </div>
              <div className={styles.rowActions}>
                <span
                  className={`${styles.badge} ${
                    brand.approvalStatus === 'APPROVED'
                      ? styles.badgeSuccess
                      : brand.approvalStatus === 'REJECTED'
                        ? styles.badgeDanger
                        : styles.badgeWarning
                  }`}
                  data-brand-status={brand.approvalStatus}
                >
                  {statusCopy.has(brand.approvalStatus)
                    ? statusCopy(brand.approvalStatus)
                    : brand.approvalStatus}
                </span>
                <BrandApprovalActions brandId={brand.id} status={brand.approvalStatus} />
              </div>
            </li>
          ))}
        </ul>
      )}

      <CreateBrandForm locale={locale} />
    </div>
  );
}

function BrandApprovalActions({
  brandId,
  status,
}: {
  brandId: string;
  status: BrandSummary['approvalStatus'];
}) {
  const t = useTranslations('taxonomyAdmin.brands.approval');
  const serverErrors = useTranslations('errors.server') as unknown as Translator;
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  async function decide(next: BrandApprovalRequest['status'], key: string) {
    setError(null);
    setBusy(key);
    try {
      const response = await sendJson(
        `/api/taxonomy/brands/${encodeURIComponent(brandId)}/approval`,
        'POST',
        { status: next } satisfies BrandApprovalRequest,
      );
      if (response.ok) {
        router.refresh();
        return;
      }
      setError(translateTaxonomyError(await readBffError(response), serverErrors));
    } catch {
      setError(serverErrors('unexpected'));
    } finally {
      setBusy(null);
    }
  }

  if (status === 'APPROVED' || status === 'REJECTED') {
    return error ? <Alert tone="danger">{error}</Alert> : null;
  }

  return (
    <div className={styles.rowActions}>
      {error ? <Alert tone="danger">{error}</Alert> : null}
      <Button
        variant="primary"
        size="sm"
        type="button"
        loading={busy === 'approve'}
        onClick={() => void decide('APPROVED', 'approve')}
      >
        {t('approve')}
      </Button>
      <Button
        variant="secondary"
        size="sm"
        type="button"
        loading={busy === 'reject'}
        onClick={() => void decide('REJECTED', 'reject')}
      >
        {t('reject')}
      </Button>
    </div>
  );
}

function CreateBrandForm({ locale }: { locale: string }) {
  const t = useTranslations('taxonomyAdmin.brands.create');
  const typeCopy = useTranslations('taxonomyAdmin.brandType');
  const common = useTranslations('taxonomyAdmin.common');
  const validate = useTranslations('taxonomyAdmin.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const schema = useMemo(() => createBrandSchema(validate), [validate]);
  const defaults: CreateBrandValues = {
    code: '',
    brandType: 'GENERAL',
    canonicalName: '',
    displayName: '',
    ownerCompany: '',
    manufacturer: '',
    countryCode: '',
    website: '',
    originalLocale: locale,
  };
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateBrandValues>({ resolver: zodResolver(schema), defaultValues: defaults });

  const [formError, setFormError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setNotice(null);
    const body: CreateBrandRequest = {
      code: values.code.trim(),
      brandType: values.brandType,
      canonicalName: values.canonicalName.trim(),
      originalLocale: values.originalLocale,
    };
    const displayName = values.displayName?.trim();
    if (displayName) body.displayName = displayName;
    const ownerCompany = values.ownerCompany?.trim();
    if (ownerCompany) body.ownerCompany = ownerCompany;
    const manufacturer = values.manufacturer?.trim();
    if (manufacturer) body.manufacturer = manufacturer;
    if (values.countryCode) body.countryCode = values.countryCode;
    const website = values.website?.trim();
    if (website) body.website = website;

    try {
      const response = await sendJson('/api/taxonomy/brands', 'POST', body);
      if (response.ok) {
        setNotice(t('created'));
        reset(defaults);
        router.refresh();
        return;
      }
      setFormError(translateTaxonomyError(await readBffError(response), serverErrors));
    } catch {
      setFormError(serverErrors('unexpected'));
    }
  });

  return (
    <form className={styles.form} method="post" onSubmit={onSubmit} noValidate aria-label={t('title')}>
      <h3 className={styles.subheading}>{t('title')}</h3>
      {formError ? <Alert tone="danger">{formError}</Alert> : null}
      {notice ? <Alert tone="success">{notice}</Alert> : null}

      <div className={styles.grid}>
        <FormField label={t('codeLabel')} htmlFor="brand-code" error={errors.code?.message} hint={t('codeHint')}>
          <input id="brand-code" className={styles.input} type="text" {...register('code')} />
        </FormField>
        <FormField label={t('canonicalNameLabel')} htmlFor="brand-canonical" error={errors.canonicalName?.message}>
          <input id="brand-canonical" className={styles.input} type="text" {...register('canonicalName')} />
        </FormField>
        <FormField label={t('brandTypeLabel')} htmlFor="brand-type" error={errors.brandType?.message}>
          <select id="brand-type" className={styles.select} {...register('brandType')}>
            {BRAND_TYPES.map((code) => (
              <option key={code} value={code}>
                {typeCopy(code)}
              </option>
            ))}
          </select>
        </FormField>
        <FormField label={t('displayNameLabel')} htmlFor="brand-display" error={errors.displayName?.message}>
          <input id="brand-display" className={styles.input} type="text" {...register('displayName')} />
        </FormField>
        <FormField label={t('countryLabel')} htmlFor="brand-country" error={errors.countryCode?.message}>
          <input id="brand-country" className={styles.input} type="text" {...register('countryCode')} />
        </FormField>
        <FormField label={t('websiteLabel')} htmlFor="brand-website" error={errors.website?.message}>
          <input id="brand-website" className={styles.input} type="text" {...register('website')} />
        </FormField>
        <FormField label={common('originalLocaleLabel')} htmlFor="brand-locale" error={errors.originalLocale?.message}>
          <select id="brand-locale" className={styles.select} {...register('originalLocale')}>
            {LOCALES.map((code) => (
              <option key={code} value={code}>
                {LOCALE_ENDONYMS[code]}
              </option>
            ))}
          </select>
        </FormField>
      </div>

      <div className={styles.actions}>
        <Button variant="primary" size="md" type="submit" loading={isSubmitting}>
          {t('submit')}
        </Button>
      </div>
    </form>
  );
}
