'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type {
  CreateEnumerationRequest,
  EnumerationSummary,
} from '@spexcrafters/api-client';
import { Alert, Button, FormField } from '@spexcrafters/ui';

import type { TranslateFn, Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';

import { translateTaxonomyError } from './errors';
import {
  createEnumerationSchema,
  type CreateEnumerationValues,
} from './schemas';
import styles from './taxonomy.module.css';

interface EnumerationListProps {
  enumerations: EnumerationSummary[];
}

/**
 * Enumeration list + create. Adding values is served by its BFF route
 * (`/api/taxonomy/enumerations/[id]/values`) but is not surfaced inline here:
 * the frozen public read contract never exposes the enumeration entity uuid
 * (EnumerationSummary/Detail carry only `code`), and the admin values endpoint
 * requires a uuid path param — so there is no id to bind an inline form to.
 */
export function EnumerationList({ enumerations }: EnumerationListProps) {
  const t = useTranslations('taxonomyAdmin.enumerations');

  return (
    <div className={styles.section} id="enumerations">
      <h2 className={styles.sectionTitle}>{t('title')}</h2>
      <p className={styles.intro}>{t('intro')}</p>

      {enumerations.length === 0 ? (
        <p className={styles.empty}>{t('empty')}</p>
      ) : (
        <ul className={styles.list}>
          {enumerations.map((en) => (
            <li key={en.code} className={styles.row} data-enumeration-code={en.code}>
              <div className={styles.rowMain}>
                <span className={`${styles.rowName} ${styles.code}`}>{en.code}</span>
                <span className={styles.rowMeta}>
                  {t('valueCount', { count: en.valueCount })}
                </span>
              </div>
            </li>
          ))}
        </ul>
      )}

      <CreateEnumerationForm />
    </div>
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
