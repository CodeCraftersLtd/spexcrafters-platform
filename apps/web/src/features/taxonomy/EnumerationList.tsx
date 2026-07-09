'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type {
  AddEnumerationValueRequest,
  CreateEnumerationRequest,
  EnumerationDetail,
  EnumerationValueView,
} from '@spexcrafters/api-client';
import { Alert, Button, FormField } from '@spexcrafters/ui';

import type { TranslateFn, Translator } from '@/i18n/translator';
import { LOCALE_ENDONYMS, LOCALES } from '@/i18n/locales';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';

import { translateTaxonomyError } from './errors';
import {
  addEnumerationValueSchema,
  createEnumerationSchema,
  type AddEnumerationValueValues,
  type CreateEnumerationValues,
} from './schemas';
import { TranslationEditorPanel } from './TranslationEditorPanel';
import styles from './taxonomy.module.css';

interface EnumerationListProps {
  /**
   * `listAdminEnumerations` result — ALL enumerations (incl. inactive), each with
   * its uuid and its values (incl. inactive/deprecated). The uuid is what unlocks
   * value administration (add value / translate value) inline.
   */
  enumerations: EnumerationDetail[];
  /** Page locale — the default authoring language for the add-value form. */
  locale: string;
}

export function EnumerationList({ enumerations, locale }: EnumerationListProps) {
  const t = useTranslations('taxonomyAdmin.enumerations');
  const common = useTranslations('taxonomyAdmin.common');

  return (
    <div className={styles.section} id="enumerations">
      <h2 className={styles.sectionTitle}>{t('title')}</h2>
      <p className={styles.intro}>{t('intro')}</p>

      {enumerations.length === 0 ? (
        <p className={styles.empty}>{t('empty')}</p>
      ) : (
        <ul className={styles.list}>
          {enumerations.map((en) => (
            <EnumerationRow
              key={en.code}
              enumeration={en}
              locale={locale}
              activeLabel={common('active')}
              inactiveLabel={common('inactive')}
              deprecatedLabel={common('deprecated')}
            />
          ))}
        </ul>
      )}

      <CreateEnumerationForm />
    </div>
  );
}

function EnumerationRow({
  enumeration,
  locale,
  activeLabel,
  inactiveLabel,
  deprecatedLabel,
}: {
  enumeration: EnumerationDetail;
  locale: string;
  activeLabel: string;
  inactiveLabel: string;
  deprecatedLabel: string;
}) {
  const t = useTranslations('taxonomyAdmin.enumerations');
  const addLabel = useTranslations('taxonomyAdmin.enumerations.addValue')('title');
  const [showAdd, setShowAdd] = useState(false);

  return (
    <li
      className={`${styles.row}${enumeration.active ? '' : ` ${styles.inactive}`}`}
      data-enumeration-code={enumeration.code}
    >
      <div className={styles.rowMain}>
        <span className={`${styles.rowName} ${styles.code}`}>{enumeration.code}</span>
        <span className={styles.rowMeta}>
          {t('valueCount', { count: enumeration.values.length })}
        </span>
        <span
          className={`${styles.badge} ${enumeration.active ? styles.badgeSuccess : styles.badgeNeutral}`}
        >
          {enumeration.active ? activeLabel : inactiveLabel}
        </span>
        <Button variant="quiet" size="sm" type="button" onClick={() => setShowAdd((open) => !open)}>
          {addLabel}
        </Button>
      </div>

      {enumeration.values.length > 0 ? (
        <ul className={styles.valueList}>
          {enumeration.values.map((value) => (
            <EnumerationValueRow
              key={value.code}
              value={value}
              deprecatedLabel={deprecatedLabel}
              inactiveLabel={inactiveLabel}
            />
          ))}
        </ul>
      ) : null}

      {showAdd ? <AddEnumerationValueForm enumerationId={enumeration.id} locale={locale} /> : null}
    </li>
  );
}

function EnumerationValueRow({
  value,
  deprecatedLabel,
  inactiveLabel,
}: {
  value: EnumerationValueView;
  deprecatedLabel: string;
  inactiveLabel: string;
}) {
  const translateLabel = useTranslations('taxonomyAdmin.translations')('title');
  const [showTranslate, setShowTranslate] = useState(false);

  return (
    <li
      className={`${styles.valueRow}${value.active ? '' : ` ${styles.inactive}`}`}
      data-enumeration-value-code={value.code}
    >
      <span className={styles.rowName}>{value.label}</span>
      <span className={`${styles.badge} ${styles.code}`}>{value.code}</span>
      {value.deprecated ? (
        <span className={`${styles.badge} ${styles.badgeWarning}`}>{deprecatedLabel}</span>
      ) : null}
      {!value.active ? (
        <span className={`${styles.badge} ${styles.badgeNeutral}`}>{inactiveLabel}</span>
      ) : null}
      <Button variant="quiet" size="sm" type="button" onClick={() => setShowTranslate((open) => !open)}>
        {translateLabel}
      </Button>
      {showTranslate ? (
        <TranslationEditorPanel
          basePath="/api/taxonomy/enumeration-values"
          resourceId={value.id}
        />
      ) : null}
    </li>
  );
}

function AddEnumerationValueForm({
  enumerationId,
  locale,
}: {
  enumerationId: string;
  locale: string;
}) {
  const t = useTranslations('taxonomyAdmin.enumerations.addValue');
  const common = useTranslations('taxonomyAdmin.common');
  const validate = useTranslations('taxonomyAdmin.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const schema = useMemo(() => addEnumerationValueSchema(validate), [validate]);
  const defaults: AddEnumerationValueValues = {
    code: '',
    label: '',
    originalLocale: locale,
    sortOrder: '',
    description: '',
  };
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<AddEnumerationValueValues>({ resolver: zodResolver(schema), defaultValues: defaults });

  const [formError, setFormError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setNotice(null);
    const body: AddEnumerationValueRequest = {
      code: values.code.trim(),
      label: values.label.trim(),
      originalLocale: values.originalLocale,
    };
    if (values.sortOrder) {
      body.sortOrder = Number(values.sortOrder);
    }
    const description = values.description?.trim();
    if (description) {
      body.description = description;
    }
    try {
      const response = await sendJson(
        `/api/taxonomy/enumerations/${encodeURIComponent(enumerationId)}/values`,
        'POST',
        body,
      );
      if (response.ok) {
        setNotice(t('added'));
        reset(defaults);
        router.refresh();
        return;
      }
      setFormError(translateTaxonomyError(await readBffError(response), serverErrors));
    } catch {
      setFormError(serverErrors('unexpected'));
    }
  });

  const idBase = `enum-value-${enumerationId}`;

  return (
    <form className={styles.form} method="post" onSubmit={onSubmit} noValidate aria-label={t('title')}>
      <h4 className={styles.subheading}>{t('title')}</h4>
      {formError ? <Alert tone="danger">{formError}</Alert> : null}
      {notice ? <Alert tone="success">{notice}</Alert> : null}

      <div className={styles.grid}>
        <FormField label={t('codeLabel')} htmlFor={`${idBase}-code`} error={errors.code?.message}>
          <input id={`${idBase}-code`} className={styles.input} type="text" {...register('code')} />
        </FormField>
        <FormField label={t('labelLabel')} htmlFor={`${idBase}-label`} error={errors.label?.message}>
          <input id={`${idBase}-label`} className={styles.input} type="text" {...register('label')} />
        </FormField>
        <FormField
          label={common('originalLocaleLabel')}
          htmlFor={`${idBase}-locale`}
          error={errors.originalLocale?.message}
        >
          <select id={`${idBase}-locale`} className={styles.select} {...register('originalLocale')}>
            {LOCALES.map((code) => (
              <option key={code} value={code}>
                {LOCALE_ENDONYMS[code]}
              </option>
            ))}
          </select>
        </FormField>
        <FormField
          label={t('sortOrderLabel')}
          htmlFor={`${idBase}-sort`}
          error={errors.sortOrder?.message}
        >
          <input
            id={`${idBase}-sort`}
            className={styles.input}
            type="text"
            inputMode="numeric"
            {...register('sortOrder')}
          />
        </FormField>
      </div>

      <div className={styles.actions}>
        <Button variant="secondary" size="md" type="submit" loading={isSubmitting}>
          {t('submit')}
        </Button>
      </div>
    </form>
  );
}

function CreateEnumerationForm() {
  const t = useTranslations('taxonomyAdmin.enumerations.create');
  const validate = useTranslations('taxonomyAdmin.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const schema = useMemo(() => createEnumerationSchema(validate), [validate]);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateEnumerationValues>({ resolver: zodResolver(schema), defaultValues: { code: '' } });

  const [formError, setFormError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setNotice(null);
    const body: CreateEnumerationRequest = { code: values.code.trim() };
    try {
      const response = await sendJson('/api/taxonomy/enumerations', 'POST', body);
      if (response.ok) {
        setNotice(t('created'));
        reset({ code: '' });
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
      <FormField label={t('codeLabel')} htmlFor="enum-code" error={errors.code?.message} hint={t('codeHint')}>
        <input id="enum-code" className={styles.input} type="text" {...register('code')} />
      </FormField>
      <div className={styles.actions}>
        <Button variant="primary" size="md" type="submit" loading={isSubmitting}>
          {t('submit')}
        </Button>
      </div>
    </form>
  );
}
