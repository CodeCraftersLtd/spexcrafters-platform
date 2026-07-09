'use client';

import Link from 'next/link';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { Alert, Button, FormField, Input } from '@spexcrafters/ui';

import type { SupportedLocale } from '@/i18n/locales';
import type { TranslateFn, Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError, translateError } from '@/features/auth/client-errors';
import { createLoginSchema, type LoginFormValues } from '@/features/auth/schemas';

import styles from './auth-forms.module.css';

interface LoginFormProps {
  locale: SupportedLocale;
  /** Sanitized internal path to navigate to after sign-in (defaults to the buyer dashboard). */
  returnTo?: string | undefined;
}

const FIELD_NAMES: ReadonlyArray<keyof LoginFormValues> = ['email', 'password'];

export function LoginForm({ locale, returnTo }: LoginFormProps) {
  const t = useTranslations('auth.login');
  const validate = useTranslations('auth.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('auth.serverErrors') as unknown as Translator;

  const schema = useMemo(() => createLoginSchema(validate), [validate]);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({ resolver: zodResolver(schema) });

  const [formError, setFormError] = useState<string | null>(null);
  const [navigating, setNavigating] = useState(false);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    let response: Response;
    try {
      response = await sendJson('/api/auth/login', 'POST', values);
    } catch {
      setFormError(serverErrors('unexpected'));
      return;
    }

    if (response.ok) {
      // Full navigation so every Server Component re-renders with the new
      // sc_session cookie.
      setNavigating(true);
      window.location.assign(returnTo ?? `/${locale}/buyer`);
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

  return (
    <form className={styles.form} method="post" onSubmit={onSubmit} noValidate>
      {formError ? <Alert tone="danger">{formError}</Alert> : null}

      <FormField label={t('emailLabel')} htmlFor="login-email" error={errors.email?.message}>
        <Input
          id="login-email"
          type="email"
          autoComplete="email"
          invalid={Boolean(errors.email)}
          aria-describedby={errors.email ? 'login-email-error' : undefined}
          {...register('email')}
        />
      </FormField>

      <FormField
        label={t('passwordLabel')}
        htmlFor="login-password"
        error={errors.password?.message}
      >
        <Input
          id="login-password"
          type="password"
          autoComplete="current-password"
          invalid={Boolean(errors.password)}
          aria-describedby={errors.password ? 'login-password-error' : undefined}
          {...register('password')}
        />
      </FormField>

      <div className={styles.actions}>
        <Button
          variant="primary"
          size="md"
          type="submit"
          loading={isSubmitting || navigating}
        >
          {t('submit')}
        </Button>
      </div>

      <p className={styles.alternate}>
        {t('noAccount')}{' '}
        <Link href={`/${locale}/auth/register`}>{t('createAccount')}</Link>
      </p>
    </form>
  );
}
