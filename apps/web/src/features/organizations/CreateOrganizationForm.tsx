'use client';

import Link from 'next/link';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type { OrganizationResponse } from '@spexcrafters/api-client';
import { Alert, Button, FormField, Input } from '@spexcrafters/ui';

import type { SupportedLocale } from '@/i18n/locales';
import type { TranslateFn, Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';
import { translateOrgError } from '@/features/organizations/org-errors';
import {
  createOrganizationSchema,
  type CreateOrganizationFormValues,
} from '@/features/organizations/schemas';

import styles from './org-components.module.css';

interface CreateOrganizationFormProps {
  locale: SupportedLocale;
}

const FIELD_NAMES: ReadonlyArray<keyof CreateOrganizationFormValues> = [
  'name',
  'type',
  'country',
];

export function CreateOrganizationForm({ locale }: CreateOrganizationFormProps) {
  const t = useTranslations('organizations.create');
  const types = useTranslations('organizations.types');
  const validate = useTranslations('organizations.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations(
    'organizations.serverErrors',
  ) as unknown as Translator;

  const schema = useMemo(() => createOrganizationSchema(validate), [validate]);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<CreateOrganizationFormValues>({
    resolver: zodResolver(schema),
    defaultValues: { type: 'BUYER' },
  });

  const [formError, setFormError] = useState<string | null>(null);
  const [navigating, setNavigating] = useState(false);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    let response: Response;
    try {
      response = await sendJson('/api/orgs', 'POST', {
        name: values.name,
        type: values.type,
        ...(values.country ? { country: values.country } : {}),
      });
    } catch {
      setFormError(serverErrors('unexpected'));
      return;
    }

    if (response.ok) {
      const organization = (await response.json()) as OrganizationResponse;
      // Full navigation so the workspace Server Component renders fresh data.
      setNavigating(true);
      window.location.assign(`/${locale}/organizations/${organization.id}`);
      return;
    }

    const error = await readBffError(response);
    let mappedToField = false;
    if (error.fields) {
      for (const name of FIELD_NAMES) {
        const fieldError = error.fields[name];
        if (fieldError) {
          setError(name, {
            type: 'server',
            message: translateOrgError(fieldError, serverErrors),
          });
          mappedToField = true;
        }
      }
    }
    if (!mappedToField) {
      setFormError(translateOrgError(error, serverErrors));
    }
  });

  return (
    <form className={styles.form} method="post" onSubmit={onSubmit} noValidate>
      {formError ? <Alert tone="danger">{formError}</Alert> : null}

      <FormField
        label={t('nameLabel')}
        htmlFor="org-name"
        hint={t('nameHint')}
        error={errors.name?.message}
      >
        <Input
          id="org-name"
          type="text"
          autoComplete="organization"
          invalid={Boolean(errors.name)}
          aria-describedby={errors.name ? 'org-name-error' : undefined}
          {...register('name')}
        />
      </FormField>

      <FormField label={t('typeLabel')} htmlFor="org-type" error={errors.type?.message}>
        <select
          id="org-type"
          className={styles.select}
          aria-invalid={errors.type ? true : undefined}
          aria-describedby={errors.type ? 'org-type-error' : undefined}
          {...register('type')}
        >
          <option value="BUYER">{types('BUYER')}</option>
          <option value="SUPPLIER">{types('SUPPLIER')}</option>
          <option value="HYBRID">{types('HYBRID')}</option>
        </select>
      </FormField>

      <FormField
        label={t('countryLabel')}
        htmlFor="org-country"
        hint={t('countryHint')}
        error={errors.country?.message}
      >
        <Input
          id="org-country"
          type="text"
          autoComplete="country"
          maxLength={2}
          invalid={Boolean(errors.country)}
          aria-describedby={errors.country ? 'org-country-error' : undefined}
          {...register('country')}
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
        <Link href={`/${locale}/organizations`}>{t('cancel')}</Link>
      </div>
    </form>
  );
}
