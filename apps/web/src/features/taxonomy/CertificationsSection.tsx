'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type {
  Certification,
  CreateCertificationRequest,
} from '@spexcrafters/api-client';
import { Alert, Button, FormField } from '@spexcrafters/ui';

import type { TranslateFn, Translator } from '@/i18n/translator';
import { LOCALE_ENDONYMS, LOCALES } from '@/i18n/locales';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';

import { CERTIFICATION_CATEGORIES } from './constants';
import { translateTaxonomyError } from './errors';
import {
  createCertificationSchema,
  type CreateCertificationValues,
} from './schemas';
import styles from './taxonomy.module.css';

interface CertificationsSectionProps {
  certifications: Certification[];
  locale: string;
}

export function CertificationsSection({
  certifications,
  locale,
}: CertificationsSectionProps) {
  const t = useTranslations('taxonomyAdmin.certifications');
  const categoryCopy = useTranslations('taxonomyAdmin.certificationCategory') as unknown as Translator;

  return (
    <div className={styles.section} id="certifications">
      <h2 className={styles.sectionTitle}>{t('title')}</h2>
      <p className={styles.intro}>{t('intro')}</p>

      {certifications.length === 0 ? (
        <p className={styles.empty}>{t('empty')}</p>
      ) : (
        <ul className={styles.list}>
          {certifications.map((cert) => (
            <li key={cert.code} className={styles.row} data-certification-code={cert.code}>
              <div className={styles.rowMain}>
                <span className={styles.rowName}>{cert.name}</span>
                <span className={styles.rowMeta}>
                  <span className={styles.code}>{cert.code}</span>
                  {cert.category && categoryCopy.has(cert.category)
                    ? ` · ${categoryCopy(cert.category)}`
                    : ''}
                </span>
              </div>
            </li>
          ))}
        </ul>
      )}

      <CreateCertificationForm locale={locale} />
    </div>
  );
}

function CreateCertificationForm({ locale }: { locale: string }) {
  const t = useTranslations('taxonomyAdmin.certifications.create');
  const categoryCopy = useTranslations('taxonomyAdmin.certificationCategory');
  const common = useTranslations('taxonomyAdmin.common');
  const validate = useTranslations('taxonomyAdmin.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const schema = useMemo(() => createCertificationSchema(validate), [validate]);
  const defaults: CreateCertificationValues = {
    code: '',
    category: '',
    countryScope: '',
    validityMonths: '',
    originalLocale: locale,
    name: '',
    description: '',
  };
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateCertificationValues>({
    resolver: zodResolver(schema),
    defaultValues: defaults,
  });

  const [formError, setFormError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setNotice(null);
    const body: CreateCertificationRequest = {
      code: values.code.trim(),
      originalLocale: values.originalLocale,
      name: values.name.trim(),
    };
    if (values.category) {
      body.category = values.category;
    }
    if (values.countryScope) {
      body.countryScope = values.countryScope;
    }
    if (values.validityMonths) {
      body.validityMonths = Number(values.validityMonths);
    }
    const description = values.description?.trim();
    if (description) {
      body.description = description;
    }
    try {
      const response = await sendJson('/api/taxonomy/certifications', 'POST', body);
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
        <FormField label={t('codeLabel')} htmlFor="cert-code" error={errors.code?.message} hint={t('codeHint')}>
          <input id="cert-code" className={styles.input} type="text" {...register('code')} />
        </FormField>
        <FormField label={t('nameLabel')} htmlFor="cert-name" error={errors.name?.message}>
          <input id="cert-name" className={styles.input} type="text" {...register('name')} />
        </FormField>
        <FormField label={t('categoryLabel')} htmlFor="cert-category" error={errors.category?.message}>
          <select id="cert-category" className={styles.select} {...register('category')}>
            <option value="">—</option>
            {CERTIFICATION_CATEGORIES.map((code) => (
              <option key={code} value={code}>
                {categoryCopy(code)}
              </option>
            ))}
          </select>
        </FormField>
        <FormField label={t('countryScopeLabel')} htmlFor="cert-country" error={errors.countryScope?.message}>
          <input id="cert-country" className={styles.input} type="text" {...register('countryScope')} />
        </FormField>
        <FormField label={t('validityMonthsLabel')} htmlFor="cert-validity" error={errors.validityMonths?.message}>
          <input id="cert-validity" className={styles.input} type="text" inputMode="numeric" {...register('validityMonths')} />
        </FormField>
        <FormField label={common('originalLocaleLabel')} htmlFor="cert-locale" error={errors.originalLocale?.message}>
          <select id="cert-locale" className={styles.select} {...register('originalLocale')}>
            {LOCALES.map((code) => (
              <option key={code} value={code}>
                {LOCALE_ENDONYMS[code]}
              </option>
            ))}
          </select>
        </FormField>
      </div>

      <FormField label={t('descriptionLabel')} htmlFor="cert-description" error={errors.description?.message}>
        <textarea id="cert-description" className={styles.textarea} {...register('description')} />
      </FormField>

      <div className={styles.actions}>
        <Button variant="primary" size="md" type="submit" loading={isSubmitting}>
          {t('submit')}
        </Button>
      </div>
    </form>
  );
}
