'use client';

import Link from 'next/link';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type { SupplierApplication } from '@spexcrafters/api-client';
import { Alert, Button, FormField, Input } from '@spexcrafters/ui';

import type { SupportedLocale } from '@/i18n/locales';
import type { TranslateFn, Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';
import { translateSupplierError } from '@/features/suppliers/errors';
import {
  createApplicationSchema,
  type CreateApplicationValues,
} from '@/features/suppliers/schemas';

import styles from './supplier.module.css';

export interface OrgOption {
  id: string;
  name: string;
}

export interface LocaleOption {
  code: string;
  label: string;
}

interface CreateApplicationFormProps {
  locale: SupportedLocale;
  organizations: OrgOption[];
  locales: LocaleOption[];
}

export function CreateApplicationForm({
  locale,
  organizations,
  locales,
}: CreateApplicationFormProps) {
  const t = useTranslations('suppliers.home');
  const validate = useTranslations('suppliers.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const schema = useMemo(() => createApplicationSchema(validate), [validate]);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CreateApplicationValues>({
    resolver: zodResolver(schema),
    defaultValues: { originalLocale: locale },
  });

  const [organizationId, setOrganizationId] = useState(organizations[0]?.id ?? '');
  const [formError, setFormError] = useState<string | null>(null);
  const [navigating, setNavigating] = useState(false);

  if (organizations.length === 0) {
    return (
      <Alert tone="info">
        {t('noOrgs')}{' '}
        <Link href={`/${locale}/organizations`}>{t('createOrg')}</Link>
      </Alert>
    );
  }

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    let response: Response;
    try {
      response = await sendJson('/api/supplier/applications', 'POST', {
        organizationId,
        originalLocale: values.originalLocale,
        legalName: values.legalName,
      });
    } catch {
      setFormError(serverErrors('unexpected'));
      return;
    }
    if (response.ok) {
      const application = (await response.json()) as SupplierApplication;
      setNavigating(true);
      window.location.assign(
        `/${locale}/supplier/applications/${application.applicationId}`,
      );
      return;
    }
    setFormError(translateSupplierError(await readBffError(response), serverErrors));
  });

  return (
    <form className={styles.form} method="post" onSubmit={onSubmit} noValidate>
      {formError ? <Alert tone="danger">{formError}</Alert> : null}

      <FormField label={t('orgLabel')} htmlFor="app-org">
        <select
          id="app-org"
          className={styles.select}
          value={organizationId}
          onChange={(event) => setOrganizationId(event.target.value)}
        >
          {organizations.map((org) => (
            <option key={org.id} value={org.id}>
              {org.name}
            </option>
          ))}
        </select>
      </FormField>

      <FormField
        label={t('originalLocaleLabel')}
        htmlFor="app-locale"
        hint={t('originalLocaleHint')}
        error={errors.originalLocale?.message}
      >
        <select
          id="app-locale"
          className={styles.select}
          {...register('originalLocale')}
        >
          {locales.map((option) => (
            <option key={option.code} value={option.code}>
              {option.label}
            </option>
          ))}
        </select>
      </FormField>

      <FormField
        label={t('legalNameLabel')}
        htmlFor="app-legal-name"
        hint={t('legalNameHint')}
        error={errors.legalName?.message}
      >
        <Input
          id="app-legal-name"
          type="text"
          dir="ltr"
          invalid={Boolean(errors.legalName)}
          {...register('legalName')}
        />
      </FormField>

      <div className={styles.actions}>
        <Button
          variant="primary"
          size="md"
          type="submit"
          loading={isSubmitting || navigating}
        >
          {t('start')}
        </Button>
      </div>
    </form>
  );
}
