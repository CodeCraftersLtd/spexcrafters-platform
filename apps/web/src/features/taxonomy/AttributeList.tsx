'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type {
  AttributeDetail,
  CreateAttributeRequest,
} from '@spexcrafters/api-client';
import { Alert, Button, FormField } from '@spexcrafters/ui';

import type { TranslateFn, Translator } from '@/i18n/translator';
import { LOCALE_ENDONYMS, LOCALES } from '@/i18n/locales';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';

import { ATTRIBUTE_DATA_TYPES } from './constants';
import { translateTaxonomyError } from './errors';
import { createAttributeSchema, type CreateAttributeValues } from './schemas';
import styles from './taxonomy.module.css';

interface AttributeListProps {
  /** `listAdminAttributes` result — ALL attributes incl. deprecated + non-visible. */
  attributes: AttributeDetail[];
  locale: string;
}

export function AttributeList({ attributes, locale }: AttributeListProps) {
  const t = useTranslations('taxonomyAdmin.attributes');
  const dataTypeCopy = useTranslations('taxonomyAdmin.dataType') as unknown as Translator;
  const common = useTranslations('taxonomyAdmin.common');

  return (
    <div className={styles.section} id="attributes">
      <h2 className={styles.sectionTitle}>{t('title')}</h2>
      <p className={styles.intro}>{t('intro')}</p>

      {attributes.length === 0 ? (
        <p className={styles.empty}>{t('empty')}</p>
      ) : (
        <ul className={styles.list}>
          {attributes.map((attr) => (
            <li key={attr.code} className={styles.row} data-attribute-code={attr.code}>
              <div className={styles.rowMain}>
                <span className={styles.rowName}>{attr.name}</span>
                <span className={styles.rowMeta}>
                  <span className={styles.code}>{attr.code}</span>
                  {' · '}
                  {dataTypeCopy.has(attr.dataType) ? dataTypeCopy(attr.dataType) : attr.dataType}
                </span>
              </div>
              <div className={styles.rowActions}>
                {attr.deprecated ? (
                  <span className={`${styles.badge} ${styles.badgeWarning}`}>
                    {common('deprecated')}
                  </span>
                ) : null}
                <span className={styles.badge}>
                  {attr.visible ? common('active') : common('inactive')}
                </span>
              </div>
            </li>
          ))}
        </ul>
      )}

      <CreateAttributeForm locale={locale} />
    </div>
  );
}

function CreateAttributeForm({ locale }: { locale: string }) {
  const t = useTranslations('taxonomyAdmin.attributes.create');
  const common = useTranslations('taxonomyAdmin.common');
  const dataTypeCopy = useTranslations('taxonomyAdmin.dataType');
  const validate = useTranslations('taxonomyAdmin.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const schema = useMemo(() => createAttributeSchema(validate), [validate]);
  const defaults: CreateAttributeValues = {
    code: '',
    dataType: 'STRING',
    unitCode: '',
    enumerationCode: '',
    originalLocale: locale,
    name: '',
    description: '',
    searchable: false,
    filterable: false,
    visible: true,
  };
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateAttributeValues>({ resolver: zodResolver(schema), defaultValues: defaults });

  const [formError, setFormError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setNotice(null);
    const body: CreateAttributeRequest = {
      code: values.code.trim(),
      dataType: values.dataType,
      originalLocale: values.originalLocale,
      name: values.name.trim(),
      searchable: values.searchable,
      filterable: values.filterable,
      visible: values.visible,
    };
    if (values.unitCode) {
      body.unitCode = values.unitCode.trim();
    }
    if (values.enumerationCode) {
      body.enumerationCode = values.enumerationCode.trim();
    }
    const description = values.description?.trim();
    if (description) {
      body.description = description;
    }
    try {
      const response = await sendJson('/api/taxonomy/attributes', 'POST', body);
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
        <FormField label={t('codeLabel')} htmlFor="attr-code" error={errors.code?.message} hint={t('codeHint')}>
          <input id="attr-code" className={styles.input} type="text" {...register('code')} />
        </FormField>
        <FormField label={t('nameLabel')} htmlFor="attr-name" error={errors.name?.message}>
          <input id="attr-name" className={styles.input} type="text" {...register('name')} />
        </FormField>
        <FormField label={t('dataTypeLabel')} htmlFor="attr-datatype" error={errors.dataType?.message}>
          <select id="attr-datatype" className={styles.select} {...register('dataType')}>
            {ATTRIBUTE_DATA_TYPES.map((code) => (
              <option key={code} value={code}>
                {dataTypeCopy(code)}
              </option>
            ))}
          </select>
        </FormField>
        <FormField label={t('unitLabel')} htmlFor="attr-unit" error={errors.unitCode?.message}>
          <input id="attr-unit" className={styles.input} type="text" {...register('unitCode')} />
        </FormField>
        <FormField label={t('enumerationLabel')} htmlFor="attr-enum" error={errors.enumerationCode?.message}>
          <input id="attr-enum" className={styles.input} type="text" {...register('enumerationCode')} />
        </FormField>
        <FormField label={common('originalLocaleLabel')} htmlFor="attr-locale" error={errors.originalLocale?.message}>
          <select id="attr-locale" className={styles.select} {...register('originalLocale')}>
            {LOCALES.map((code) => (
              <option key={code} value={code}>
                {LOCALE_ENDONYMS[code]}
              </option>
            ))}
          </select>
        </FormField>
      </div>

      <FormField label={t('descriptionLabel')} htmlFor="attr-description" error={errors.description?.message}>
        <textarea id="attr-description" className={styles.textarea} {...register('description')} />
      </FormField>

      <div className={styles.checkboxGroup}>
        <label className={styles.checkboxLabel}>
          <input type="checkbox" {...register('searchable')} />
          {t('searchableLabel')}
        </label>
        <label className={styles.checkboxLabel}>
          <input type="checkbox" {...register('filterable')} />
          {t('filterableLabel')}
        </label>
        <label className={styles.checkboxLabel}>
          <input type="checkbox" {...register('visible')} />
          {t('visibleLabel')}
        </label>
      </div>

      <div className={styles.actions}>
        <Button variant="primary" size="md" type="submit" loading={isSubmitting}>
          {t('submit')}
        </Button>
      </div>
    </form>
  );
}
