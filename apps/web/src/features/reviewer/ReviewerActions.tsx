'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type {
  OperationalStatus,
  SupplierApplicationStatus,
} from '@spexcrafters/api-client';
import { Alert, Button, FormField, Input } from '@spexcrafters/ui';

import type { TranslateFn, Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';
import { translateSupplierError } from '@/features/suppliers/errors';
import {
  reasonSchema,
  requestChangesSchema,
  type ReasonValues,
  type RequestChangesValues,
} from '@/features/reviewer/schemas';

import styles from '@/features/suppliers/supplier.module.css';

interface ReviewerActionsProps {
  applicationId: string;
  supplierId: string;
  status: SupplierApplicationStatus;
  operationalStatus: OperationalStatus;
}

type Panel = 'requestChanges' | 'reject' | 'suspend' | null;

export function ReviewerActions({
  applicationId,
  supplierId,
  status,
  operationalStatus,
}: ReviewerActionsProps) {
  const t = useTranslations('reviewer.actions');
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [panel, setPanel] = useState<Panel>(null);

  const canClaim = status === 'SUBMITTED' || status === 'RESUBMITTED';
  const canDecide = status === 'UNDER_REVIEW';
  const canSuspend = operationalStatus === 'ACTIVE';

  async function simplePost(url: string, key: string, successKey: string) {
    setError(null);
    setNotice(null);
    setBusy(key);
    try {
      const response = await sendJson(url, 'POST');
      if (response.ok) {
        setNotice(t(successKey));
        router.refresh();
        return;
      }
      setError(translateSupplierError(await readBffError(response), serverErrors));
    } catch {
      setError(serverErrors('unexpected'));
    } finally {
      setBusy(null);
    }
  }

  return (
    <div className={styles.stack}>
      {error ? <Alert tone="danger">{error}</Alert> : null}
      {notice ? <Alert tone="success">{notice}</Alert> : null}

      <div className={styles.actions}>
        {canClaim ? (
          <Button
            variant="primary"
            size="md"
            type="button"
            loading={busy === 'claim'}
            onClick={() =>
              void simplePost(
                `/api/reviewer/${encodeURIComponent(applicationId)}/claim`,
                'claim',
                'claimed',
              )
            }
          >
            {t('claim')}
          </Button>
        ) : null}
        {canDecide ? (
          <>
            <Button
              variant="primary"
              size="md"
              type="button"
              loading={busy === 'approve'}
              onClick={() =>
                void simplePost(
                  `/api/reviewer/${encodeURIComponent(applicationId)}/approve`,
                  'approve',
                  'approved',
                )
              }
            >
              {t('approve')}
            </Button>
            <Button variant="secondary" size="md" type="button" onClick={() => setPanel(panel === 'requestChanges' ? null : 'requestChanges')}>
              {t('requestChanges')}
            </Button>
            <Button variant="secondary" size="md" type="button" onClick={() => setPanel(panel === 'reject' ? null : 'reject')}>
              {t('reject')}
            </Button>
          </>
        ) : null}
        {canSuspend ? (
          <Button variant="secondary" size="md" type="button" onClick={() => setPanel(panel === 'suspend' ? null : 'suspend')}>
            {t('suspendSupplier')}
          </Button>
        ) : null}
      </div>

      {panel === 'requestChanges' ? (
        <RequestChangesForm applicationId={applicationId} onDone={() => setPanel(null)} />
      ) : null}
      {panel === 'reject' ? (
        <ReasonForm
          url={`/api/reviewer/${encodeURIComponent(applicationId)}/reject`}
          namespace="reject"
          onDone={() => setPanel(null)}
        />
      ) : null}
      {panel === 'suspend' ? (
        <ReasonForm
          url={`/api/reviewer/suppliers/${encodeURIComponent(supplierId)}/suspend`}
          namespace="suspend"
          onDone={() => setPanel(null)}
        />
      ) : null}
    </div>
  );
}

function RequestChangesForm({
  applicationId,
  onDone,
}: {
  applicationId: string;
  onDone: () => void;
}) {
  const t = useTranslations('reviewer.requestChanges');
  const validate = useTranslations('reviewer.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('errors.server') as unknown as Translator;
  const router = useRouter();
  const schema = useMemo(() => requestChangesSchema(validate), [validate]);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RequestChangesValues>({ resolver: zodResolver(schema) });
  const [formError, setFormError] = useState<string | null>(null);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      const response = await sendJson(
        `/api/reviewer/${encodeURIComponent(applicationId)}/request-changes`,
        'POST',
        values,
      );
      if (response.ok) {
        onDone();
        router.refresh();
        return;
      }
      setFormError(translateSupplierError(await readBffError(response), serverErrors));
    } catch {
      setFormError(serverErrors('unexpected'));
    }
  });

  return (
    <form className={styles.form} method="post" onSubmit={onSubmit} noValidate aria-label={t('title')}>
      <h4 className={styles.subheading}>{t('title')}</h4>
      {formError ? <Alert tone="danger">{formError}</Alert> : null}
      <FormField label={t('requestedItemLabel')} htmlFor="rc-item" error={errors.requestedItem?.message}>
        <Input id="rc-item" type="text" {...register('requestedItem')} />
      </FormField>
      <FormField label={t('reasonLabel')} htmlFor="rc-reason" error={errors.reason?.message}>
        <textarea id="rc-reason" className={styles.textarea} {...register('reason')} />
      </FormField>
      <div className={styles.actions}>
        <Button variant="secondary" size="md" type="submit" loading={isSubmitting}>
          {t('submit')}
        </Button>
      </div>
    </form>
  );
}

function ReasonForm({
  url,
  namespace,
  onDone,
}: {
  url: string;
  namespace: 'reject' | 'suspend';
  onDone: () => void;
}) {
  const t = useTranslations(`reviewer.${namespace}`);
  const validate = useTranslations('reviewer.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('errors.server') as unknown as Translator;
  const router = useRouter();
  const schema = useMemo(() => reasonSchema(validate), [validate]);
  const {
    register,
    handleSubmit,
    formState: { isSubmitting },
  } = useForm<ReasonValues>({ resolver: zodResolver(schema) });
  const [formError, setFormError] = useState<string | null>(null);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      const response = await sendJson(url, 'POST', values.reason ? { reason: values.reason } : {});
      if (response.ok || response.status === 204) {
        onDone();
        router.refresh();
        return;
      }
      setFormError(translateSupplierError(await readBffError(response), serverErrors));
    } catch {
      setFormError(serverErrors('unexpected'));
    }
  });

  return (
    <form className={styles.form} method="post" onSubmit={onSubmit} noValidate aria-label={t('title')}>
      <h4 className={styles.subheading}>{t('title')}</h4>
      {formError ? <Alert tone="danger">{formError}</Alert> : null}
      <FormField label={t('reasonLabel')} htmlFor={`reason-${namespace}`}>
        <textarea id={`reason-${namespace}`} className={styles.textarea} {...register('reason')} />
      </FormField>
      <div className={styles.actions}>
        <Button variant="secondary" size="md" type="submit" loading={isSubmitting}>
          {t('submit')}
        </Button>
      </div>
    </form>
  );
}
