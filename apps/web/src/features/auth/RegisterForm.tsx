'use client';

import Link from 'next/link';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { Alert, Button, FormField, Input } from '@spexcrafters/ui';

import { interpolate, type Dictionary, type Locale } from '@/lib/i18n';
import { sendJson } from '@/lib/csrf-client';
import { readBffError, translateError } from '@/features/auth/client-errors';
import {
  createRegisterSchema,
  type RegisterFormValues,
} from '@/features/auth/schemas';

import styles from './auth-forms.module.css';

interface RegisterFormProps {
  locale: Locale;
  copy: Dictionary['auth']['register'];
  validation: Dictionary['auth']['validation'];
  serverErrors: Dictionary['auth']['serverErrors'];
}

const FIELD_NAMES: ReadonlyArray<keyof RegisterFormValues> = [
  'displayName',
  'email',
  'password',
];

export function RegisterForm({
  locale,
  copy,
  validation,
  serverErrors,
}: RegisterFormProps) {
  const schema = useMemo(() => createRegisterSchema(validation), [validation]);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({ resolver: zodResolver(schema) });

  const [formError, setFormError] = useState<string | null>(null);
  const [registeredEmail, setRegisteredEmail] = useState<string | null>(null);
  const [resendState, setResendState] = useState<'idle' | 'sending' | 'sent'>('idle');

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    let response: Response;
    try {
      response = await sendJson('/api/auth/register', 'POST', { ...values, locale });
    } catch {
      setFormError(serverErrors.unexpected);
      return;
    }

    if (response.status === 201) {
      setRegisteredEmail(values.email);
      return;
    }

    const error = await readBffError(response);
    let mappedToField = false;
    if (error.fields) {
      for (const name of FIELD_NAMES) {
        const fieldError = error.fields[name];
        if (fieldError) {
          setError(name, { type: 'server', message: translateError(fieldError, serverErrors) });
          mappedToField = true;
        }
      }
    }
    if (!mappedToField) {
      setFormError(translateError(error, serverErrors));
    }
  });

  async function resend() {
    if (!registeredEmail || resendState === 'sending') {
      return;
    }
    setResendState('sending');
    try {
      await sendJson('/api/auth/resend-verification', 'POST', {
        email: registeredEmail,
      });
    } finally {
      setResendState('sent');
    }
  }

  if (registeredEmail) {
    return (
      <div className={styles.stack} aria-live="polite">
        <Alert tone="success" title={copy.checkEmail.title}>
          {interpolate(copy.checkEmail.body, { email: registeredEmail })}
        </Alert>
        {resendState === 'sent' ? (
          <Alert tone="info">
            {interpolate(copy.checkEmail.resent, { email: registeredEmail })}
          </Alert>
        ) : (
          <div className={styles.statusActions}>
            <Button
              variant="secondary"
              size="md"
              type="button"
              loading={resendState === 'sending'}
              onClick={resend}
            >
              {copy.checkEmail.resend}
            </Button>
          </div>
        )}
      </div>
    );
  }

  return (
    <form className={styles.form} method="post" onSubmit={onSubmit} noValidate>
      {formError ? <Alert tone="danger">{formError}</Alert> : null}

      <FormField
        label={copy.displayNameLabel}
        htmlFor="register-displayName"
        hint={copy.displayNameHint}
        error={errors.displayName?.message}
      >
        <Input
          id="register-displayName"
          type="text"
          autoComplete="name"
          invalid={Boolean(errors.displayName)}
          aria-describedby={errors.displayName ? 'register-displayName-error' : undefined}
          {...register('displayName')}
        />
      </FormField>

      <FormField
        label={copy.emailLabel}
        htmlFor="register-email"
        error={errors.email?.message}
      >
        <Input
          id="register-email"
          type="email"
          autoComplete="email"
          invalid={Boolean(errors.email)}
          aria-describedby={errors.email ? 'register-email-error' : undefined}
          {...register('email')}
        />
      </FormField>

      <FormField
        label={copy.passwordLabel}
        htmlFor="register-password"
        hint={copy.passwordHint}
        error={errors.password?.message}
      >
        <Input
          id="register-password"
          type="password"
          autoComplete="new-password"
          invalid={Boolean(errors.password)}
          aria-describedby={errors.password ? 'register-password-error' : undefined}
          {...register('password')}
        />
      </FormField>

      <div className={styles.actions}>
        <Button variant="primary" size="md" type="submit" loading={isSubmitting}>
          {copy.submit}
        </Button>
      </div>

      <p className={styles.alternate}>
        {copy.haveAccount}{' '}
        <Link href={`/${locale}/auth/login`}>{copy.signIn}</Link>
      </p>
    </form>
  );
}
