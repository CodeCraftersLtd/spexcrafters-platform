'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type { ReviewRequest } from '@spexcrafters/api-client';
import { Alert, Button, FormField } from '@spexcrafters/ui';

import type { SupportedLocale } from '@/i18n/locales';
import type { TranslateFn, Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';
import { translateSupplierError } from '@/features/suppliers/errors';
import {
  respondChangeRequestSchema,
  type RespondChangeRequestValues,
} from '@/features/suppliers/schemas';

import styles from './supplier.module.css';

interface ChangeRequestsSectionProps {
  locale: SupportedLocale;
  applicationId: string;
  changeRequests: ReviewRequest[];
  canRespond: boolean;
}

export function ChangeRequestsSection({
  locale,
  applicationId,
  changeRequests,
  canRespond,
}: ChangeRequestsSectionProps) {
  const t = useTranslations('suppliers.changeRequests');

  const dateFormatter = useMemo(
    () => new Intl.DateTimeFormat(locale, { dateStyle: 'medium' }),
    [locale],
  );

  return (
    <section className={styles.stack} aria-label={t('title')}>
      <p className={styles.intro}>{t('intro')}</p>
      {changeRequests.length === 0 ? (
        <p className={styles.empty}>{t('empty')}</p>
      ) : (
        <ul className={styles.list}>
          {changeRequests.map((request) => (
            <li key={request.id} className={styles.section}>
              <div className={styles.rowMain}>
                <span className={styles.summaryValue}>{request.requestedItem}</span>
                <span className={styles.rowMeta}>
                  {t('requestedAt', {
                    date: dateFormatter.format(new Date(request.requestedAt)),
                  })}{' '}
                  · {t(`state.${request.status}`)}
                </span>
              </div>
              <p>{request.reason}</p>
              {request.supplierResponse ? (
                <p className={styles.rowMeta}>
                  {t('yourResponse')}: {request.supplierResponse}
                </p>
              ) : null}
              {canRespond && request.status === 'OPEN' ? (
                <RespondForm applicationId={applicationId} changeRequestId={request.id} />
              ) : null}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

interface RespondFormProps {
  applicationId: string;
  changeRequestId: string;
}

function RespondForm({ applicationId, changeRequestId }: RespondFormProps) {
  const t = useTranslations('suppliers.changeRequests');
  const validate = useTranslations('suppliers.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const schema = useMemo(() => respondChangeRequestSchema(validate), [validate]);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RespondChangeRequestValues>({ resolver: zodResolver(schema) });

  const [formError, setFormError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setSent(false);
    let response: Response;
    try {
      response = await sendJson(
        `/api/supplier/applications/${encodeURIComponent(applicationId)}/change-requests/${encodeURIComponent(changeRequestId)}/respond`,
        'POST',
        { response: values.response },
      );
    } catch {
      setFormError(serverErrors('unexpected'));
      return;
    }
    if (response.ok) {
      setSent(true);
      router.refresh();
      return;
    }
    setFormError(translateSupplierError(await readBffError(response), serverErrors));
  });

  return (
    <form className={styles.form} method="post" onSubmit={onSubmit} noValidate>
      {formError ? <Alert tone="danger">{formError}</Alert> : null}
      {sent ? <Alert tone="success">{t('responded')}</Alert> : null}
      <FormField label={t('responseLabel')} htmlFor={`respond-${changeRequestId}`} error={errors.response?.message}>
        <textarea id={`respond-${changeRequestId}`} className={styles.textarea} {...register('response')} />
      </FormField>
      <div className={styles.actions}>
        <Button variant="secondary" size="md" type="submit" loading={isSubmitting}>
          {t('respond')}
        </Button>
      </div>
    </form>
  );
}
