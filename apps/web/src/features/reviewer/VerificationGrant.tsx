'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type { Evidence, VerificationStatus } from '@spexcrafters/api-client';
import { Alert, Button, FormField } from '@spexcrafters/ui';

import type { TranslateFn, Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';
import { translateSupplierError } from '@/features/suppliers/errors';
import { grantScopeSchema, type GrantScopeValues } from '@/features/reviewer/schemas';
import { taxonomyLabel, VERIFICATION_SCOPE_CODES } from '@/features/suppliers/taxonomy';

import styles from '@/features/suppliers/supplier.module.css';

interface VerificationGrantProps {
  supplierId: string;
  evidence: Evidence[];
  verification: VerificationStatus | null;
}

export function VerificationGrant({
  supplierId,
  evidence,
  verification,
}: VerificationGrantProps) {
  const t = useTranslations('reviewer.grant');
  const scopeT = useTranslations('reviewer.scope');
  const actions = useTranslations('reviewer.actions');
  const verificationCopy = useTranslations('verification.status');
  const taxonomy = useTranslations('taxonomy') as unknown as Translator;
  const validate = useTranslations('reviewer.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const schema = useMemo(() => grantScopeSchema(validate), [validate]);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<GrantScopeValues>({
    resolver: zodResolver(schema),
    defaultValues: { evidenceIds: [] },
  });

  const [scopeCode, setScopeCode] = useState<string>(VERIFICATION_SCOPE_CODES[0]);
  const [formError, setFormError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busyScope, setBusyScope] = useState<string | null>(null);

  const finalized = evidence.filter((item) => item.uploadState === 'FINALIZED');

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setNotice(null);
    try {
      const response = await sendJson(
        `/api/reviewer/suppliers/${encodeURIComponent(supplierId)}/verification/scopes/${encodeURIComponent(scopeCode)}/grant`,
        'POST',
        {
          evidenceIds: values.evidenceIds,
          ...(values.reason ? { reason: values.reason } : {}),
        },
      );
      if (response.ok) {
        setNotice(actions('grantedScope'));
        router.refresh();
        return;
      }
      setFormError(translateSupplierError(await readBffError(response), serverErrors));
    } catch {
      setFormError(serverErrors('unexpected'));
    }
  });

  async function scopeAction(code: string, action: 'suspend' | 'revoke') {
    setFormError(null);
    setNotice(null);
    setBusyScope(`${code}:${action}`);
    try {
      const response = await sendJson(
        `/api/reviewer/suppliers/${encodeURIComponent(supplierId)}/verification/scopes/${encodeURIComponent(code)}/${action}`,
        'POST',
        {},
      );
      if (response.ok) {
        setNotice(actions(action === 'suspend' ? 'suspendedScope' : 'revokedScope'));
        router.refresh();
        return;
      }
      setFormError(translateSupplierError(await readBffError(response), serverErrors));
    } catch {
      setFormError(serverErrors('unexpected'));
    } finally {
      setBusyScope(null);
    }
  }

  return (
    <div className={styles.stack}>
      {formError ? <Alert tone="danger">{formError}</Alert> : null}
      {notice ? <Alert tone="success">{notice}</Alert> : null}

      {verification && verification.scopes.length > 0 ? (
        <ul className={styles.list}>
          {verification.scopes.map((scope) => (
            <li key={scope.scopeCode} className={styles.row}>
              <div className={styles.rowMain}>
                <span className={styles.rowName}>
                  {taxonomyLabel(taxonomy, 'verificationScope', scope.scopeCode)}
                </span>
                <span className={styles.rowMeta}>{verificationCopy(`state.${scope.status}`)}</span>
              </div>
              <div className={styles.rowActions}>
                {scope.status === 'VERIFIED' ? (
                  <>
                    <Button variant="secondary" size="sm" type="button" loading={busyScope === `${scope.scopeCode}:suspend`} onClick={() => void scopeAction(scope.scopeCode, 'suspend')}>
                      {scopeT('suspendSubmit')}
                    </Button>
                    <Button variant="secondary" size="sm" type="button" loading={busyScope === `${scope.scopeCode}:revoke`} onClick={() => void scopeAction(scope.scopeCode, 'revoke')}>
                      {scopeT('revokeSubmit')}
                    </Button>
                  </>
                ) : null}
              </div>
            </li>
          ))}
        </ul>
      ) : null}

      {finalized.length === 0 ? (
        <Alert tone="info">{t('noEvidence')}</Alert>
      ) : (
        <form className={styles.form} method="post" onSubmit={onSubmit} noValidate aria-label={t('title')}>
          <h4 className={styles.subheading}>{t('title')}</h4>
          <FormField label={t('scopeLabel')} htmlFor="grant-scope">
            <select
              id="grant-scope"
              className={styles.select}
              value={scopeCode}
              onChange={(event) => setScopeCode(event.target.value)}
            >
              {VERIFICATION_SCOPE_CODES.map((code) => (
                <option key={code} value={code}>{taxonomyLabel(taxonomy, 'verificationScope', code)}</option>
              ))}
            </select>
          </FormField>

          <fieldset className={styles.stack}>
            <legend>{t('evidenceLabel')}</legend>
            <p className={styles.intro}>{t('evidenceHint')}</p>
            {errors.evidenceIds ? (
              <p role="alert" className={styles.rowMeta}>{errors.evidenceIds.message}</p>
            ) : null}
            <div className={styles.checkboxGroup}>
              {finalized.map((item) => (
                <label key={item.id} className={styles.checkboxLabel}>
                  <input type="checkbox" value={item.id} {...register('evidenceIds')} />
                  <span dir="ltr">{item.originalFilename}</span>
                </label>
              ))}
            </div>
          </fieldset>

          <FormField label={t('reasonLabel')} htmlFor="grant-reason">
            <textarea id="grant-reason" className={styles.textarea} {...register('reason')} />
          </FormField>

          <div className={styles.actions}>
            <Button variant="primary" size="md" type="submit" loading={isSubmitting}>
              {t('submit')}
            </Button>
          </div>
        </form>
      )}
    </div>
  );
}
