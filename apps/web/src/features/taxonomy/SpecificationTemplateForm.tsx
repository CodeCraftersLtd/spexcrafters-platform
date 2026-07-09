'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useFieldArray, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type {
  AttributeSummary,
  PutSpecificationTemplateRequest,
} from '@spexcrafters/api-client';
import { Alert, Button, FormField } from '@spexcrafters/ui';

import type { TranslateFn, Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';

import { translateTaxonomyError } from './errors';
import {
  specificationTemplateSchema,
  type SpecificationTemplateValues,
} from './schemas';
import styles from './taxonomy.module.css';

export interface TemplateCategoryOption {
  id: string;
  code: string;
  name: string;
  depth: number;
}

interface SpecificationTemplateFormProps {
  categories: TemplateCategoryOption[];
  attributes: AttributeSummary[];
}

export function SpecificationTemplateForm({
  categories,
  attributes,
}: SpecificationTemplateFormProps) {
  const t = useTranslations('taxonomyAdmin.specificationTemplate');
  const validate = useTranslations('taxonomyAdmin.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const schema = useMemo(() => specificationTemplateSchema(validate), [validate]);
  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<SpecificationTemplateValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      categoryId: '',
      code: '',
      attributes: [{ attributeCode: '', required: true, sortOrder: '', defaultValue: '' }],
    },
  });
  const { fields, append, remove } = useFieldArray({ control, name: 'attributes' });

  const [formError, setFormError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setNotice(null);
    const body: PutSpecificationTemplateRequest = {
      code: values.code.trim(),
      attributes: values.attributes.map((row, index) => ({
        attributeCode: row.attributeCode,
        required: row.required,
        sortOrder: row.sortOrder ? Number(row.sortOrder) : index,
        ...(row.defaultValue?.trim() ? { defaultValue: row.defaultValue.trim() } : {}),
      })),
    };
    try {
      const response = await sendJson(
        `/api/taxonomy/categories/${encodeURIComponent(values.categoryId)}/specification-template`,
        'PUT',
        body,
      );
      if (response.ok) {
        setNotice(t('saved'));
        reset({
          categoryId: values.categoryId,
          code: '',
          attributes: [{ attributeCode: '', required: true, sortOrder: '', defaultValue: '' }],
        });
        router.refresh();
        return;
      }
      setFormError(translateTaxonomyError(await readBffError(response), serverErrors));
    } catch {
      setFormError(serverErrors('unexpected'));
    }
  });

  const blocked = categories.length === 0 || attributes.length === 0;

  return (
    <div className={styles.section} id="specification-template">
      <h2 className={styles.sectionTitle}>{t('title')}</h2>
      <p className={styles.intro}>{t('intro')}</p>

      {categories.length === 0 ? (
        <p className={styles.empty}>{t('noCategories')}</p>
      ) : attributes.length === 0 ? (
        <p className={styles.empty}>{t('noAttributes')}</p>
      ) : null}

      <form className={styles.form} method="post" onSubmit={onSubmit} noValidate aria-label={t('title')}>
        {formError ? <Alert tone="danger">{formError}</Alert> : null}
        {notice ? <Alert tone="success">{notice}</Alert> : null}

        <div className={styles.grid}>
          <FormField label={t('categoryLabel')} htmlFor="tmpl-category" error={errors.categoryId?.message}>
            <select id="tmpl-category" className={styles.select} {...register('categoryId')}>
              <option value="">—</option>
              {categories.map((category) => (
                <option key={category.code} value={category.id}>
                  {`${'—'.repeat(category.depth)} ${category.name} (${category.code})`}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label={t('templateCodeLabel')} htmlFor="tmpl-code" error={errors.code?.message}>
            <input id="tmpl-code" className={styles.input} type="text" {...register('code')} />
          </FormField>
        </div>

        <h3 className={styles.subheading}>{t('attributesLabel')}</h3>
        {errors.attributes?.root?.message ? (
          <Alert tone="danger">{errors.attributes.root.message}</Alert>
        ) : null}

        <ul className={styles.list}>
          {fields.map((field, index) => (
            <li key={field.id} className={styles.templateRow}>
              <FormField
                label={t('attributeLabel')}
                htmlFor={`tmpl-attr-${index}`}
                error={errors.attributes?.[index]?.attributeCode?.message}
              >
                <select id={`tmpl-attr-${index}`} className={styles.select} {...register(`attributes.${index}.attributeCode`)}>
                  <option value="">—</option>
                  {attributes.map((attr) => (
                    <option key={attr.code} value={attr.code}>
                      {`${attr.name} (${attr.code})`}
                    </option>
                  ))}
                </select>
              </FormField>
              <label className={styles.checkboxLabel}>
                <input type="checkbox" {...register(`attributes.${index}.required`)} />
                {t('requiredLabel')}
              </label>
              <FormField
                label={t('sortOrderLabel')}
                htmlFor={`tmpl-sort-${index}`}
                error={errors.attributes?.[index]?.sortOrder?.message}
              >
                <input
                  id={`tmpl-sort-${index}`}
                  className={styles.input}
                  type="text"
                  inputMode="numeric"
                  {...register(`attributes.${index}.sortOrder`)}
                />
              </FormField>
              <Button
                variant="quiet"
                size="sm"
                type="button"
                onClick={() => remove(index)}
                disabled={fields.length === 1}
              >
                {t('removeRow')}
              </Button>
            </li>
          ))}
        </ul>

        <div className={styles.actions}>
          <Button
            variant="secondary"
            size="sm"
            type="button"
            onClick={() =>
              append({ attributeCode: '', required: true, sortOrder: '', defaultValue: '' })
            }
          >
            {t('addRow')}
          </Button>
          <Button variant="primary" size="md" type="submit" loading={isSubmitting} disabled={blocked}>
            {t('submit')}
          </Button>
        </div>
      </form>
    </div>
  );
}
