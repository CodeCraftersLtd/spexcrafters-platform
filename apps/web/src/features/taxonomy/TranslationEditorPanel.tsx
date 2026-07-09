'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type { TranslationUpsertRequest } from '@spexcrafters/api-client';
import { Alert, Button, FormField } from '@spexcrafters/ui';

import type { TranslateFn, Translator } from '@/i18n/translator';
import { LOCALE_ENDONYMS, LOCALES } from '@/i18n/locales';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';

import { translateTaxonomyError } from './errors';
import {
  translationUpsertSchema,
  type TranslationUpsertValues,
} from './schemas';
import styles from './taxonomy.module.css';

interface TranslationEditorPanelProps {
  /** BFF collection base, e.g. `/api/taxonomy/categories`. */
  basePath: string;
  /** Resource id (uuid) whose translations are edited. */
  resourceId: string;
  /** When true, an approve control is shown (categories only). */
  supportsApprove?: boolean;
}

/**
 * Reusable per-locale translation editor (upsert + optional approve), mirroring
 * the Phase-7 supplier TranslationEditor. Posts the TranslationUpsertRequest to
 * `${basePath}/${resourceId}/translations/${locale}` (PUT) and, when enabled,
 * approves via `.../approve` (POST).
 */
export function TranslationEditorPanel({
  basePath,
  resourceId,
  supportsApprove = false,
}: TranslationEditorPanelProps) {
  const t = useTranslations('taxonomyAdmin.translations');
  const sourceCopy = useTranslations('taxonomyAdmin.translationSource');
  const validate = useTranslations('taxonomyAdmin.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const schema = useMemo(() => translationUpsertSchema(validate), [validate]);
  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<TranslationUpsertValues>({
    resolver: zodResolver(schema),
    defaultValues: { locale: '', name: '', description: '', source: 'HUMAN' },
  });

  const [formError, setFormError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [approving, setApproving] = useState(false);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setNotice(null);
    const body: TranslationUpsertRequest = {
      name: values.name.trim(),
      source: values.source,
    };
    const description = values.description?.trim();
    if (description) {
      body.description = description;
    }
    try {
      const response = await sendJson(
        `${basePath}/${encodeURIComponent(resourceId)}/translations/${encodeURIComponent(values.locale)}`,
        'PUT',
        body,
      );
      if (response.ok) {
        setNotice(t('saved'));
        router.refresh();
        return;
      }
      setFormError(translateTaxonomyError(await readBffError(response), serverErrors));
    } catch {
      setFormError(serverErrors('unexpected'));
    }
  });

  async function onApprove() {
    const locale = getValues('locale');
    if (!locale) {
      setFormError(validate('localeRequired'));
      return;
    }
    setFormError(null);
    setNotice(null);
    setApproving(true);
    try {
      const response = await sendJson(
        `${basePath}/${encodeURIComponent(resourceId)}/translations/${encodeURIComponent(locale)}/approve`,
        'POST',
      );
      if (response.ok) {
        setNotice(t('approved'));
        router.refresh();
        return;
      }
      setFormError(translateTaxonomyError(await readBffError(response), serverErrors));
    } catch {
      setFormError(serverErrors('unexpected'));
    } finally {
      setApproving(false);
    }
  }

  const idBase = `translation-${resourceId}`;

  return (
    <form className={styles.form} method="post" onSubmit={onSubmit} noValidate aria-label={t('title')}>
      <h4 className={styles.subheading}>{t('title')}</h4>
      {formError ? <Alert tone="danger">{formError}</Alert> : null}
      {notice ? <Alert tone="success">{notice}</Alert> : null}

      <FormField label={t('localeLabel')} htmlFor={`${idBase}-locale`} error={errors.locale?.message}>
        <select id={`${idBase}-locale`} className={styles.select} {...register('locale')}>
          <option value="">—</option>
          {LOCALES.map((code) => (
            <option key={code} value={code}>
              {LOCALE_ENDONYMS[code]}
            </option>
          ))}
        </select>
      </FormField>

      <FormField label={t('nameLabel')} htmlFor={`${idBase}-name`} error={errors.name?.message}>
        <input id={`${idBase}-name`} className={styles.input} type="text" {...register('name')} />
      </FormField>

      <FormField label={t('descriptionLabel')} htmlFor={`${idBase}-description`} error={errors.description?.message}>
        <textarea id={`${idBase}-description`} className={styles.textarea} {...register('description')} />
      </FormField>

      <FormField label={t('sourceLabel')} htmlFor={`${idBase}-source`}>
        <select id={`${idBase}-source`} className={styles.select} {...register('source')}>
          <option value="HUMAN">{sourceCopy('HUMAN')}</option>
          <option value="MACHINE">{sourceCopy('MACHINE')}</option>
          <option value="IMPORT">{sourceCopy('IMPORT')}</option>
        </select>
      </FormField>

      <div className={styles.actions}>
        <Button variant="secondary" size="md" type="submit" loading={isSubmitting}>
          {t('submit')}
        </Button>
        {supportsApprove ? (
          <Button
            variant="quiet"
            size="md"
            type="button"
            loading={approving}
            onClick={() => void onApprove()}
          >
            {t('approve')}
          </Button>
        ) : null}
      </div>
    </form>
  );
}
