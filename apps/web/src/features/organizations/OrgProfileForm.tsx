'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { Alert, Button, FormField, Input } from '@spexcrafters/ui';

import type { TranslateFn, Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';
import { translateOrgError } from '@/features/organizations/org-errors';
import {
  updateOrganizationSchema,
  type UpdateOrganizationFormValues,
} from '@/features/organizations/schemas';

import styles from './org-components.module.css';

interface OrgProfileFormProps {
  organizationId: string;
  name: string;
  country: string | undefined;
  /** Optimistic-locking version from the last read; mismatch → 409. */
  version: number;
}

/**
 * Organization profile editor — rendered only for callers holding
 * organization.update. A 409 (concurrent edit) surfaces the version-conflict
 * message; router.refresh() re-reads the current version.
 */
export function OrgProfileForm({
  organizationId,
  name,
  country,
  version,
}: OrgProfileFormProps) {
  const t = useTranslations('organizations.workspace.profile');
  const validate = useTranslations('organizations.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations(
    'organizations.serverErrors',
  ) as unknown as Translator;

  const router = useRouter();
  const schema = useMemo(() => updateOrganizationSchema(validate), [validate]);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<UpdateOrganizationFormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name, country: country ?? '' },
  });

  const [formError, setFormError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setSaved(false);
    let response: Response;
    try {
      // PATCH semantics: only provided fields change. An empty country is
      // omitted (the contract has no explicit clear operation).
      response = await sendJson(
        `/api/orgs/${encodeURIComponent(organizationId)}`,
        'PATCH',
        {
          name: values.name,
          ...(values.country ? { country: values.country } : {}),
          version,
        },
      );
    } catch {
      setFormError(serverErrors('unexpected'));
      return;
    }

    if (response.ok) {
      setSaved(true);
      router.refresh();
      return;
    }

    const error = await readBffError(response);
    let mappedToField = false;
    if (error.fields) {
      for (const field of ['name', 'country'] as const) {
        const fieldError = error.fields[field];
        if (fieldError) {
          setError(field, {
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
    if (response.status === 409) {
      // Pull the latest version so a retry can succeed.
      router.refresh();
    }
  });

  return (
    <form
      className={styles.form}
      method="post"
      onSubmit={onSubmit}
      noValidate
      aria-label={t('title')}
    >
      <h2 className={styles.subheading}>{t('title')}</h2>
      {formError ? <Alert tone="danger">{formError}</Alert> : null}
      {saved ? <Alert tone="success">{t('saved')}</Alert> : null}

      <FormField
        label={t('nameLabel')}
        htmlFor="org-profile-name"
        error={errors.name?.message}
      >
        <Input
          id="org-profile-name"
          type="text"
          invalid={Boolean(errors.name)}
          {...register('name')}
        />
      </FormField>

      <FormField
        label={t('countryLabel')}
        htmlFor="org-profile-country"
        hint={t('countryHint')}
        error={errors.country?.message}
      >
        <Input
          id="org-profile-country"
          type="text"
          maxLength={2}
          invalid={Boolean(errors.country)}
          {...register('country')}
        />
      </FormField>

      <div className={styles.actions}>
        <Button variant="secondary" size="md" type="submit" loading={isSubmitting}>
          {t('submit')}
        </Button>
      </div>
    </form>
  );
}
