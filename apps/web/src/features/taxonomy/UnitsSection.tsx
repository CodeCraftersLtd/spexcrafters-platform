'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type { CreateUnitRequest, Unit } from '@spexcrafters/api-client';
import { Alert, Button, FormField } from '@spexcrafters/ui';

import type { TranslateFn, Translator } from '@/i18n/translator';
import { LOCALE_ENDONYMS, LOCALES } from '@/i18n/locales';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';

import { UNIT_FAMILIES } from './constants';
import { translateTaxonomyError } from './errors';
import { createUnitSchema, type CreateUnitValues } from './schemas';
import styles from './taxonomy.module.css';

interface UnitsSectionProps {
  units: Unit[];
  locale: string;
}

export function UnitsSection({ units, locale }: UnitsSectionProps) {
  const t = useTranslations('taxonomyAdmin.units');
  const familyCopy = useTranslations('taxonomyAdmin.unitFamily') as unknown as Translator;

  return (
    <div className={styles.section} id="units">
      <h2 className={styles.sectionTitle}>{t('title')}</h2>
      <p className={styles.intro}>{t('intro')}</p>

      {units.length === 0 ? (
        <p className={styles.empty}>{t('empty')}</p>
      ) : (
        <ul className={styles.list}>
          {units.map((unit) => (
            <li key={unit.code} className={styles.row} data-unit-code={unit.code}>
              <div className={styles.rowMain}>
                <span className={styles.rowName}>{unit.displayName}</span>
                <span className={styles.rowMeta}>
                  <span className={styles.code}>{unit.code}</span>
                  {' · '}
                  {familyCopy.has(unit.family) ? familyCopy(unit.family) : unit.family}
                </span>
              </div>
            </li>
          ))}
        </ul>
      )}

      <CreateUnitForm locale={locale} />
    </div>
  );
}

function CreateUnitForm({ locale }: { locale: string }) {
  const t = useTranslations('taxonomyAdmin.units.create');
  const familyCopy = useTranslations('taxonomyAdmin.unitFamily');
  const common = useTranslations('taxonomyAdmin.common');
  const validate = useTranslations('taxonomyAdmin.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const schema = useMemo(() => createUnitSchema(validate), [validate]);
  const defaults: CreateUnitValues = {
    code: '',
    family: 'LENGTH',
    baseUnitCode: '',
    factorToBase: '',
    originalLocale: locale,
    displayName: '',
  };
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateUnitValues>({ resolver: zodResolver(schema), defaultValues: defaults });

  const [formError, setFormError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setNotice(null);
    const body: CreateUnitRequest = {
      code: values.code.trim(),
      family: values.family,
      originalLocale: values.originalLocale,
      displayName: values.displayName.trim(),
    };
    if (values.baseUnitCode) {
      body.baseUnitCode = values.baseUnitCode.trim();
    }
    if (values.factorToBase) {
      body.factorToBase = Number(values.factorToBase);
    }
    try {
      const response = await sendJson('/api/taxonomy/units', 'POST', body);
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
        <FormField label={t('codeLabel')} htmlFor="unit-code" error={errors.code?.message} hint={t('codeHint')}>
          <input id="unit-code" className={styles.input} type="text" {...register('code')} />
        </FormField>
        <FormField label={t('displayNameLabel')} htmlFor="unit-display" error={errors.displayName?.message}>
          <input id="unit-display" className={styles.input} type="text" {...register('displayName')} />
        </FormField>
        <FormField label={t('familyLabel')} htmlFor="unit-family" error={errors.family?.message}>
          <select id="unit-family" className={styles.select} {...register('family')}>
            {UNIT_FAMILIES.map((code) => (
              <option key={code} value={code}>
                {familyCopy(code)}
              </option>
            ))}
          </select>
        </FormField>
        <FormField label={t('baseUnitLabel')} htmlFor="unit-base" error={errors.baseUnitCode?.message}>
          <input id="unit-base" className={styles.input} type="text" {...register('baseUnitCode')} />
        </FormField>
        <FormField label={t('factorToBaseLabel')} htmlFor="unit-factor" error={errors.factorToBase?.message}>
          <input id="unit-factor" className={styles.input} type="text" inputMode="decimal" {...register('factorToBase')} />
        </FormField>
        <FormField label={common('originalLocaleLabel')} htmlFor="unit-locale" error={errors.originalLocale?.message}>
          <select id="unit-locale" className={styles.select} {...register('originalLocale')}>
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
