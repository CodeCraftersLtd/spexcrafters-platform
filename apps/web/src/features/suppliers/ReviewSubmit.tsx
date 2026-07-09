'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useState } from 'react';

import type { SupplierApplicationStatus } from '@spexcrafters/api-client';
import { Alert, Button } from '@spexcrafters/ui';

import type { Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';
import { translateSupplierError } from '@/features/suppliers/errors';
import type { CompletenessCheck } from '@/features/suppliers/status';

import styles from './supplier.module.css';

interface ReviewSubmitProps {
  applicationId: string;
  status: SupplierApplicationStatus;
  checks: CompletenessCheck[];
  canSubmit: boolean;
  canWithdraw: boolean;
}

export function ReviewSubmit({
  applicationId,
  status,
  checks,
  canSubmit,
  canWithdraw,
}: ReviewSubmitProps) {
  const t = useTranslations('suppliers.review');
  const c = useTranslations('suppliers.completeness');
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState<'submit' | 'withdraw' | null>(null);

  const done = checks.filter((check) => check.complete).length;
  const allComplete = done === checks.length;
  const isResubmit = status === 'CHANGES_REQUESTED';

  async function act(action: 'submit' | 'withdraw') {
    setError(null);
    setNotice(null);
    setBusy(action);
    try {
      const response = await sendJson(
        `/api/supplier/applications/${encodeURIComponent(applicationId)}/${action}`,
        'POST',
      );
      if (response.ok) {
        setNotice(action === 'submit' ? t('submitted') : t('withdrawn'));
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
    <section className={styles.stack} aria-label={t('title')}>
      <p className={styles.intro}>{t('intro')}</p>
      {error ? <Alert tone="danger">{error}</Alert> : null}
      {notice ? <Alert tone="success">{notice}</Alert> : null}

      <div className={styles.completeness}>
        <p className={styles.summaryValue}>{c('ratio', { done, total: checks.length })}</p>
        <ul className={styles.checklist}>
          {checks.map((check) => (
            <li key={check.key} className={`${styles.checkItem} ${check.complete ? styles.checkDone : styles.checkPending}`}>
              <span aria-hidden="true">{check.complete ? '✓' : '○'}</span>
              <span>{c(`item.${check.key}`)}</span>
              <span className={styles.rowMeta}>{check.complete ? c('complete') : c('incomplete')}</span>
            </li>
          ))}
        </ul>
      </div>

      {!allComplete ? <Alert tone="warning">{t('incompleteWarning')}</Alert> : null}

      <div className={styles.actions}>
        {canSubmit ? (
          <Button variant="primary" size="md" type="button" loading={busy === 'submit'} onClick={() => void act('submit')}>
            {isResubmit ? t('resubmit') : t('submit')}
          </Button>
        ) : null}
        {canWithdraw ? (
          <Button variant="secondary" size="md" type="button" loading={busy === 'withdraw'} onClick={() => void act('withdraw')}>
            {t('withdraw')}
          </Button>
        ) : null}
      </div>
    </section>
  );
}
