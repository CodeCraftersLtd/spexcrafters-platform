'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type { AddFacilityRequest, Facility } from '@spexcrafters/api-client';
import { Alert, Button, FormField, Input } from '@spexcrafters/ui';

import type { TranslateFn, Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';
import { translateSupplierError } from '@/features/suppliers/errors';
import { facilitySchema, type FacilityValues } from '@/features/suppliers/schemas';
import { FACILITY_TYPE_CODES, taxonomyLabel } from '@/features/suppliers/taxonomy';

import styles from './supplier.module.css';

interface FacilitiesSectionProps {
  supplierId: string;
  facilities: Facility[];
  canUpdate: boolean;
}

export function FacilitiesSection({
  supplierId,
  facilities,
  canUpdate,
}: FacilitiesSectionProps) {
  const t = useTranslations('suppliers.facilities');
  const taxonomy = useTranslations('taxonomy') as unknown as Translator;
  const validate = useTranslations('suppliers.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const schema = useMemo(() => facilitySchema(validate), [validate]);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FacilityValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      facilityTypeCode: 'FACTORY',
      addressPrivacy: 'PUBLIC_CITY',
      ownership: 'OWNED',
      isPublic: true,
    },
  });

  const [formError, setFormError] = useState<string | null>(null);
  const [added, setAdded] = useState(false);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setAdded(false);
    const payload: AddFacilityRequest = {
      facilityTypeCode: values.facilityTypeCode,
      country: values.country,
      addressPrivacy: values.addressPrivacy,
      ownership: values.ownership,
      isPublic: values.isPublic,
      ...(values.region ? { region: values.region } : {}),
      ...(values.city ? { city: values.city } : {}),
      ...(values.name ? { name: values.name } : {}),
      ...(values.description ? { description: values.description } : {}),
    };
    let response: Response;
    try {
      response = await sendJson(
        `/api/supplier/${encodeURIComponent(supplierId)}/facilities`,
        'POST',
        payload,
      );
    } catch {
      setFormError(serverErrors('unexpected'));
      return;
    }
    if (response.ok) {
      setAdded(true);
      reset({
        facilityTypeCode: 'FACTORY',
        addressPrivacy: 'PUBLIC_CITY',
        ownership: 'OWNED',
        isPublic: true,
        country: '',
      });
      router.refresh();
      return;
    }
    setFormError(translateSupplierError(await readBffError(response), serverErrors));
  });

  return (
    <section className={styles.stack} aria-label={t('title')}>
      <p className={styles.intro}>{t('intro')}</p>

      {facilities.length === 0 ? (
        <p className={styles.empty}>{t('empty')}</p>
      ) : (
        <ul className={styles.list}>
          {facilities.map((facility) => {
            const original = facility.translations.find((row) => row.original);
            return (
              <li key={facility.id} className={styles.row}>
                <div className={styles.rowMain}>
                  <span className={styles.summaryValue}>
                    {original?.name ?? taxonomyLabel(taxonomy, 'facilityType', facility.facilityTypeCode)}
                  </span>
                  <span className={styles.rowMeta}>
                    {taxonomyLabel(taxonomy, 'facilityType', facility.facilityTypeCode)} ·{' '}
                    {facility.city ? `${facility.city}, ` : ''}
                    {facility.country} · {t(`ownership.${facility.ownership}`)}
                  </span>
                </div>
                <span className={styles.badge}>
                  {facility.isPublic ? t('publicBadge') : t('privateBadge')}
                </span>
              </li>
            );
          })}
        </ul>
      )}

      {canUpdate ? (
        <form className={styles.form} method="post" onSubmit={onSubmit} noValidate aria-label={t('addTitle')}>
          <h4 className={styles.subheading}>{t('addTitle')}</h4>
          {formError ? <Alert tone="danger">{formError}</Alert> : null}
          {added ? <Alert tone="success">{t('added')}</Alert> : null}
          <div className={styles.grid}>
            <FormField label={t('facilityTypeLabel')} htmlFor="facility-type" error={errors.facilityTypeCode?.message}>
              <select id="facility-type" className={styles.select} {...register('facilityTypeCode')}>
                {FACILITY_TYPE_CODES.map((code) => (
                  <option key={code} value={code}>{taxonomyLabel(taxonomy, 'facilityType', code)}</option>
                ))}
              </select>
            </FormField>
            <FormField label={t('countryLabel')} htmlFor="facility-country" error={errors.country?.message}>
              <Input id="facility-country" type="text" maxLength={2} dir="ltr" {...register('country')} />
            </FormField>
            <FormField label={t('regionLabel')} htmlFor="facility-region" error={errors.region?.message}>
              <Input id="facility-region" type="text" {...register('region')} />
            </FormField>
            <FormField label={t('cityLabel')} htmlFor="facility-city" error={errors.city?.message}>
              <Input id="facility-city" type="text" {...register('city')} />
            </FormField>
            <FormField label={t('addressPrivacyLabel')} htmlFor="facility-privacy" error={errors.addressPrivacy?.message}>
              <select id="facility-privacy" className={styles.select} {...register('addressPrivacy')}>
                <option value="PUBLIC_CITY">{t('privacy.PUBLIC_CITY')}</option>
                <option value="PRIVATE">{t('privacy.PRIVATE')}</option>
              </select>
            </FormField>
            <FormField label={t('ownershipLabel')} htmlFor="facility-ownership" error={errors.ownership?.message}>
              <select id="facility-ownership" className={styles.select} {...register('ownership')}>
                <option value="OWNED">{t('ownership.OWNED')}</option>
                <option value="LEASED">{t('ownership.LEASED')}</option>
                <option value="PARTNER">{t('ownership.PARTNER')}</option>
              </select>
            </FormField>
            <FormField label={t('nameLabel')} htmlFor="facility-name" error={errors.name?.message}>
              <Input id="facility-name" type="text" {...register('name')} />
            </FormField>
          </div>
          <FormField label={t('descriptionLabel')} htmlFor="facility-description" error={errors.description?.message}>
            <textarea id="facility-description" className={styles.textarea} {...register('description')} />
          </FormField>
          <label className={styles.checkboxLabel}>
            <input type="checkbox" {...register('isPublic')} />
            {t('isPublicLabel')}
          </label>
          <div className={styles.actions}>
            <Button variant="secondary" size="md" type="submit" loading={isSubmitting}>
              {t('add')}
            </Button>
          </div>
        </form>
      ) : null}
    </section>
  );
}
