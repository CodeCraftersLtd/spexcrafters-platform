'use client';

import Link from 'next/link';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { Alert, Button, FormField, Input } from '@spexcrafters/ui';

import type { Dictionary, Locale } from '@/lib/i18n';
import { readBffError, translateError } from '@/features/auth/client-errors';
import {
  createResendVerificationSchema,
  type ResendVerificationFormValues,
} from '@/features/auth/schemas';

import styles from './auth-forms.module.css';

interface VerifyEmailProps {
  locale: Locale;
  /** Token from the emailed link (?token=…); null when absent. */
  token: string | null;
  copy: Dictionary['auth']['verifyEmail'];
  validation: Dictionary['auth']['validation'];
  serverErrors: Dictionary['auth']['serverErrors'];
}

type VerificationState =
  | { status: 'verifying' }
  | { status: 'success' }
  | { status: 'error'; message: string };

export function VerifyEmail({
  locale,
  token,
  copy,
  validation,
  serverErrors,
}: VerifyEmailProps) {
  const [state, setState] = useState<VerificationState>(
    token
      ? { status: 'verifying' }
      : { status: 'error', message: copy.missingToken },
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
        response = await fetch('/api/auth/verify-email', {
          method: 'POST',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify({ token }),
        });
      } catch {
        if (!cancelled) {
          setState({ status: 'error', message: serverErrors.unexpected });
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
  }, [token, serverErrors]);

  if (state.status === 'verifying') {
    return (
      <p role="status" aria-live="polite">
        {copy.verifying}
      </p>
    );
  }

  if (state.status === 'success') {
    return (
      <div className={styles.stack}>
        <Alert tone="success" title={copy.successTitle}>
          {copy.successBody}
        </Alert>
        <p>
          <Link href={`/${locale}/auth/login`}>{copy.goToLogin}</Link>
        </p>
      </div>
    );
  }

  return (
    <div className={styles.stack}>
      <Alert tone="danger" title={copy.errorTitle}>
        {state.message}
      </Alert>
      <ResendForm copy={copy} validation={validation} serverErrors={serverErrors} />
    </div>
  );
}

interface ResendFormProps {
  copy: Dictionary['auth']['verifyEmail'];
  validation: Dictionary['auth']['validation'];
  serverErrors: Dictionary['auth']['serverErrors'];
}

function ResendForm({ copy, validation, serverErrors }: ResendFormProps) {
  const schema = useMemo(
    () => createResendVerificationSchema(validation),
    [validation],
  );
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResendVerificationFormValues>({ resolver: zodResolver(schema) });

  const [outcome, setOutcome] = useState<'idle' | 'accepted' | 'failed'>('idle');

  const onSubmit = handleSubmit(async (values) => {
    try {
      const response = await fetch('/api/auth/resend-verification', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify(values),
      });
      setOutcome(response.status === 202 ? 'accepted' : 'failed');
    } catch {
      setOutcome('failed');
    }
  });

  if (outcome === 'accepted') {
    return <Alert tone="info">{copy.resendAccepted}</Alert>;
  }

  return (
    <form className={styles.form} method="post" onSubmit={onSubmit} noValidate aria-label={copy.resendTitle}>
      <h2>{copy.resendTitle}</h2>
      {outcome === 'failed' ? <Alert tone="danger">{serverErrors.unexpected}</Alert> : null}
      <FormField
        label={copy.resendEmailLabel}
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
          {copy.resendSubmit}
        </Button>
      </div>
    </form>
  );
}
