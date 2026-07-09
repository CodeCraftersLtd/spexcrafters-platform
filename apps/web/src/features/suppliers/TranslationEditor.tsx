'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type { UpsertTranslationRequest } from '@spexcrafters/api-client';
import { Alert, Button, FormField } from '@spexcrafters/ui';

import type { TranslateFn, Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';
import { translateSupplierError } from '@/features/suppliers/errors';
import {
  translationSchema,
  type TranslationValues,
} from '@/features/suppliers/schemas';

import styles from './supplier.module.css';

interface TranslationEditorProps {
  supplierId: string;
  locale: string;
  /** True when editing the authoritative original-language content. */
  isOriginal: boolean;
  defaults: TranslationValues;
  onSaved?: () => void;
}

const DESCRIPTION_FIELDS = [
  'companyDescription',
  'productionCapabilityDescription',
  'oemDescription',
  'odmDescription',
  'privateLabelDescription',
  'qualityControlDescription',
  'exportMarketDescription',
] as const;

/** Shared editor for original content and per-locale translations (supplier.update). */
export function TranslationEditor({
  supplierId,
  locale,
  isOriginal,
  defaults,
  onSaved,
}: TranslationEditorProps) {
  const t = useTranslations('suppliers.content');
  const validate = useTranslations('suppliers.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const schema = useMemo(() => translationSchema(validate), [validate]);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<TranslationValues>({
    resolver: zodResolver(schema),
    defaultValues: defaults,
  });

  const [formError, setFormError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setSaved(false);
    const body: UpsertTranslationRequest = { source: 'HUMAN' };
    for (const [key, value] of Object.entries(values)) {
      const trimmed = value?.trim();
      if (trimmed) {
        (body as Record<string, unknown>)[key] = trimmed;
      }
    }
    let response: Response;
    try {
      response = await sendJson(
        `/api/supplier/${encodeURIComponent(supplierId)}/profile/translations/${encodeURIComponent(locale)}`,
        'PUT',
        body,
      );
    } catch {
      setFormError(serverErrors('unexpected'));
      return;
    }
    if (response.ok) {
      setSaved(true);
      onSaved?.();
      router.refresh();
      return;
    }
    setFormError(translateSupplierError(await readBffError(response), serverErrors));
  });

  const idPrefix = `translation-${locale}`;

  return (
    <form className={styles.form} method="post" onSubmit={onSubmit} noValidate>
      {formError ? <Alert tone="danger">{formError}</Alert> : null}
      {saved ? <Alert tone="success">{t('saved')}</Alert> : null}

      <FormField label={t('tradingNameLabel')} htmlFor={`${idPrefix}-trading`} error={errors.tradingName?.message}>
        <input id={`${idPrefix}-trading`} className={styles.select} type="text" {...register('tradingName')} />
      </FormField>

      {DESCRIPTION_FIELDS.map((field) => (
        <FormField key={field} label={t(`${field}Label`)} htmlFor={`${idPrefix}-${field}`} error={errors[field]?.message}>
          <textarea id={`${idPrefix}-${field}`} className={styles.textarea} {...register(field)} />
        </FormField>
      ))}

      <div className={styles.actions}>
        <Button variant={isOriginal ? 'primary' : 'secondary'} size="md" type="submit" loading={isSubmitting}>
          {t('save')}
        </Button>
      </div>
    </form>
  );
}
