'use client';

import Link from 'next/link';
import { useTranslations } from 'next-intl';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { Alert, Button, FormField, Input } from '@spexcrafters/ui';

import type { SupportedLocale } from '@/i18n/locales';
import type { TranslateFn, Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError, translateError } from '@/features/auth/client-errors';
import {
  createResendVerificationSchema,
  type ResendVerificationFormValues,
} from '@/features/auth/schemas';

import styles from './auth-forms.module.css';

interface VerifyEmailProps {
  locale: SupportedLocale;
  /** Token from the emailed link (?token=…); null when absent. */
  token: string | null;
}

type VerificationState =
  | { status: 'verifying' }
  | { status: 'success' }
  | { status: 'error'; message: string };

export function VerifyEmail({ locale, token }: VerifyEmailProps) {
  const t = useTranslations('auth.verifyEmail');
  const serverErrors = useTranslations('auth.serverErrors') as unknown as Translator;

  const [state, setState] = useState<VerificationState>(
    token ? { status: 'verifying' } : { status: 'error', message: t('missingToken') },
  );
  // Guards the effect against React Strict Mode double-invocation so the
  // single-use token is posted exactly once per page view (the endpoint is
  // idempotent for already-verified users, but one request is still right).
  const requested = useRef(false);

  useEffect(() => {
    if (!token || requested.current) {
      return;
    }
    requested.current = true;

    let cancelled = false;
    void (async () => {
      let response: Response;
      try {
        response = await sendJson('/api/auth/verify-email', 'POST', { token });
      } catch {
        if (!cancelled) {
          setState({ status: 'error', message: serverErrors('unexpected') });
        }
        return;
      }
      if (cancelled) {
        return;
      }
      if (response.status === 204) {
        setState({ status: 'success' });
        return;
      }
      const error = await readBffError(response);
      setState({ status: 'error', message: translateError(error, serverErrors) });
    })();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  if (state.status === 'verifying') {
    return (
      <p role="status" aria-live="polite">
        {t('verifying')}
      </p>
    );
  }

  if (state.status === 'success') {
    return (
      <div className={styles.stack}>
        <Alert tone="success" title={t('successTitle')}>
          {t('successBody')}
        </Alert>
        <p>
          <Link href={`/${locale}/auth/login`}>{t('goToLogin')}</Link>
        </p>
      </div>
    );
  }

  return (
    <div className={styles.stack}>
      <Alert tone="danger" title={t('errorTitle')}>
        {state.message}
      </Alert>
      <ResendForm />
    </div>
  );
}

function ResendForm() {
  const t = useTranslations('auth.verifyEmail');
  const validate = useTranslations('auth.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('auth.serverErrors') as unknown as Translator;

  const schema = useMemo(() => createResendVerificationSchema(validate), [validate]);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResendVerificationFormValues>({ resolver: zodResolver(schema) });

  const [outcome, setOutcome] = useState<'idle' | 'accepted' | 'failed'>('idle');

  const onSubmit = handleSubmit(async (values) => {
    try {
      const response = await sendJson(
        '/api/auth/resend-verification',
        'POST',
        values,
      );
      setOutcome(response.status === 202 ? 'accepted' : 'failed');
    } catch {
      setOutcome('failed');
    }
  });

  if (outcome === 'accepted') {
    return <Alert tone="info">{t('resendAccepted')}</Alert>;
  }

  return (
    <form
      className={styles.form}
      method="post"
      onSubmit={onSubmit}
      noValidate
      aria-label={t('resendTitle')}
    >
      <h2>{t('resendTitle')}</h2>
      {outcome === 'failed' ? <Alert tone="danger">{serverErrors('unexpected')}</Alert> : null}
      <FormField
        label={t('resendEmailLabel')}
        htmlFor="resend-email"
        error={errors.email?.message}
      >
        <Input
          id="resend-email"
          type="email"
          autoComplete="email"
          invalid={Boolean(errors.email)}
          aria-describedby={errors.email ? 'resend-email-error' : undefined}
          {...register('email')}
        />
      </FormField>
      <div className={styles.actions}>
        <Button variant="secondary" size="md" type="submit" loading={isSubmitting}>
          {t('resendSubmit')}
        </Button>
      </div>
    </form>
  );
}
