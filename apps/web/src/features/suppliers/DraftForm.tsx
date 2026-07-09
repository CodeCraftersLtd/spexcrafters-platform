'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type { UpdateSupplierDraftRequest } from '@spexcrafters/api-client';
import { Alert, Button, FormField, Input } from '@spexcrafters/ui';

import type { TranslateFn, Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';
import { translateSupplierError } from '@/features/suppliers/errors';
import {
  companyInfoSchema,
  type CompanyInfoValues,
} from '@/features/suppliers/schemas';
import {
  CAPABILITY_CODES,
  COMPANY_TYPE_CODES,
  EMPLOYEE_RANGE_CODES,
  SUPPLIER_TYPE_CODES,
  taxonomyLabel,
} from '@/features/suppliers/taxonomy';

import styles from './supplier.module.css';

interface DraftFormProps {
  applicationId: string;
  version: number;
  defaults: CompanyInfoValues;
}

function omitEmpty(value: string | undefined): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}

/** Company information + optical profile draft editor (requires supplier.update). */
export function DraftForm({ applicationId, version, defaults }: DraftFormProps) {
  const t = useTranslations('suppliers.companyInfo');
  const optical = useTranslations('suppliers.opticalProfile');
  const taxonomy = useTranslations('taxonomy') as unknown as Translator;
  const validate = useTranslations('suppliers.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const schema = useMemo(() => companyInfoSchema(validate), [validate]);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CompanyInfoValues>({
    resolver: zodResolver(schema),
    defaultValues: defaults,
  });

  const [formError, setFormError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setSaved(false);
    const payload: UpdateSupplierDraftRequest = {
      version,
      types: values.types,
      capabilities: values.capabilities,
    };
    const scalar: Array<[keyof UpdateSupplierDraftRequest, string | undefined]> = [
      ['legalName', values.legalName],
      ['registeredLegalNameTranslated', omitEmpty(values.registeredLegalNameTranslated)],
      ['tradingName', omitEmpty(values.tradingName)],
      ['registrationNumber', omitEmpty(values.registrationNumber)],
      ['countryOfRegistration', omitEmpty(values.countryOfRegistration)],
      ['registrationAuthority', omitEmpty(values.registrationAuthority)],
      ['registrationDate', omitEmpty(values.registrationDate)],
      ['companyTypeCode', omitEmpty(values.companyTypeCode)],
      ['employeeRange', omitEmpty(values.employeeRange)],
      ['website', omitEmpty(values.website)],
      ['businessEmail', omitEmpty(values.businessEmail)],
      ['businessPhone', omitEmpty(values.businessPhone)],
    ];
    for (const [key, val] of scalar) {
      if (val !== undefined) {
        (payload as Record<string, unknown>)[key] = val;
      }
    }
    const year = omitEmpty(values.yearEstablished);
    if (year) {
      payload.yearEstablished = Number(year);
    }

    let response: Response;
    try {
      response = await sendJson(
        `/api/supplier/applications/${encodeURIComponent(applicationId)}`,
        'PATCH',
        payload,
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
    setFormError(translateSupplierError(await readBffError(response), serverErrors));
    if (response.status === 409) {
      router.refresh();
    }
  });

  return (
    <form className={styles.form} method="post" onSubmit={onSubmit} noValidate aria-label={t('title')}>
      {formError ? <Alert tone="danger">{formError}</Alert> : null}
      {saved ? <Alert tone="success">{t('saved')}</Alert> : null}

      <div className={styles.grid}>
        <FormField label={t('legalNameLabel')} htmlFor="draft-legal-name" hint={t('legalNameHint')} error={errors.legalName?.message}>
          <Input id="draft-legal-name" type="text" dir="ltr" invalid={Boolean(errors.legalName)} {...register('legalName')} />
        </FormField>
        <FormField label={t('registeredLegalNameTranslatedLabel')} htmlFor="draft-legal-translated" error={errors.registeredLegalNameTranslated?.message}>
          <Input id="draft-legal-translated" type="text" {...register('registeredLegalNameTranslated')} />
        </FormField>
        <FormField label={t('tradingNameLabel')} htmlFor="draft-trading" error={errors.tradingName?.message}>
          <Input id="draft-trading" type="text" {...register('tradingName')} />
        </FormField>
        <FormField label={t('registrationNumberLabel')} htmlFor="draft-reg-number" error={errors.registrationNumber?.message}>
          <Input id="draft-reg-number" type="text" dir="ltr" {...register('registrationNumber')} />
        </FormField>
        <FormField label={t('countryLabel')} htmlFor="draft-country" hint={t('countryHint')} error={errors.countryOfRegistration?.message}>
          <Input id="draft-country" type="text" maxLength={2} dir="ltr" {...register('countryOfRegistration')} />
        </FormField>
        <FormField label={t('registrationAuthorityLabel')} htmlFor="draft-authority" error={errors.registrationAuthority?.message}>
          <Input id="draft-authority" type="text" {...register('registrationAuthority')} />
        </FormField>
        <FormField label={t('registrationDateLabel')} htmlFor="draft-reg-date" error={errors.registrationDate?.message}>
          <Input id="draft-reg-date" type="text" inputMode="numeric" placeholder="YYYY-MM-DD" dir="ltr" {...register('registrationDate')} />
        </FormField>
        <FormField label={t('companyTypeLabel')} htmlFor="draft-company-type" error={errors.companyTypeCode?.message}>
          <select id="draft-company-type" className={styles.select} {...register('companyTypeCode')}>
            <option value="">{t('companyTypePlaceholder')}</option>
            {COMPANY_TYPE_CODES.map((code) => (
              <option key={code} value={code}>{taxonomyLabel(taxonomy, 'companyType', code)}</option>
            ))}
          </select>
        </FormField>
        <FormField label={t('yearEstablishedLabel')} htmlFor="draft-year" error={errors.yearEstablished?.message}>
          <Input id="draft-year" type="text" inputMode="numeric" maxLength={4} dir="ltr" {...register('yearEstablished')} />
        </FormField>
        <FormField label={t('employeeRangeLabel')} htmlFor="draft-employees" error={errors.employeeRange?.message}>
          <select id="draft-employees" className={styles.select} {...register('employeeRange')}>
            <option value="">{t('employeeRangePlaceholder')}</option>
            {EMPLOYEE_RANGE_CODES.map((code) => (
              <option key={code} value={code}>{taxonomyLabel(taxonomy, 'employeeRange', code)}</option>
            ))}
          </select>
        </FormField>
        <FormField label={t('websiteLabel')} htmlFor="draft-website" error={errors.website?.message}>
          <Input id="draft-website" type="url" dir="ltr" {...register('website')} />
        </FormField>
        <FormField label={t('businessEmailLabel')} htmlFor="draft-email" error={errors.businessEmail?.message}>
          <Input id="draft-email" type="email" dir="ltr" {...register('businessEmail')} />
        </FormField>
        <FormField label={t('businessPhoneLabel')} htmlFor="draft-phone" error={errors.businessPhone?.message}>
          <Input id="draft-phone" type="tel" dir="ltr" {...register('businessPhone')} />
        </FormField>
      </div>

      <fieldset className={styles.stack}>
        <legend className={styles.subheading}>{optical('typesLabel')}</legend>
        <p className={styles.intro}>{optical('typesHint')}</p>
        <div className={styles.checkboxGroup}>
          {SUPPLIER_TYPE_CODES.map((code) => (
            <label key={code} className={styles.checkboxLabel}>
              <input type="checkbox" value={code} {...register('types')} />
              {taxonomyLabel(taxonomy, 'supplierType', code)}
            </label>
          ))}
        </div>
      </fieldset>

      <fieldset className={styles.stack}>
        <legend className={styles.subheading}>{optical('capabilitiesLabel')}</legend>
        <p className={styles.intro}>{optical('capabilitiesHint')}</p>
        <div className={styles.checkboxGroup}>
          {CAPABILITY_CODES.map((code) => (
            <label key={code} className={styles.checkboxLabel}>
              <input type="checkbox" value={code} {...register('capabilities')} />
              {taxonomyLabel(taxonomy, 'capability', code)}
            </label>
          ))}
        </div>
      </fieldset>

      <p className={styles.intro}>{optical('facilitiesNote')}</p>

      <div className={styles.actions}>
        <Button variant="primary" size="md" type="submit" loading={isSubmitting}>
          {t('save')}
        </Button>
      </div>
    </form>
  );
}
