'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type {
  CategoryDetail,
  CreateCategoryRequest,
} from '@spexcrafters/api-client';
import { Alert, Button, FormField } from '@spexcrafters/ui';

import type { TranslateFn, Translator } from '@/i18n/translator';
import { LOCALE_ENDONYMS, LOCALES } from '@/i18n/locales';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';

import { CATEGORY_CLASSIFICATIONS } from './constants';
import { translateTaxonomyError } from './errors';
import { createCategorySchema, type CreateCategoryValues } from './schemas';
import { TranslationEditorPanel } from './TranslationEditorPanel';
import { buildAdminCategoryTree, previewSlug, type AdminCategoryNode } from './tree';
import styles from './taxonomy.module.css';

interface CategoryTreeProps {
  /**
   * Flat, path-ordered `listAdminCategories` result (ALL categories including
   * inactive). Each row carries its uuid, so translation actions target the id
   * directly — no per-category detail fan-out.
   */
  categories: CategoryDetail[];
  /** Page locale — the default authoring language for the create form. */
  locale: string;
}

export function CategoryTree({ categories, locale }: CategoryTreeProps) {
  const t = useTranslations('taxonomyAdmin.categories');
  const classificationCopy = useTranslations('taxonomyAdmin.classification');
  const common = useTranslations('taxonomyAdmin.common');
  const translateLabel = useTranslations('taxonomyAdmin.translations')('title');

  const tree = useMemo(() => buildAdminCategoryTree(categories), [categories]);
  const parentOptions = useMemo(
    () =>
      categories.map((category) => ({
        code: category.code,
        name: category.name,
        depth: category.depth,
      })),
    [categories],
  );

  return (
    <div className={styles.section} id="categories">
      <h2 className={styles.sectionTitle}>{t('title')}</h2>
      <p className={styles.intro}>{t('intro')}</p>

      {tree.length === 0 ? (
        <p className={styles.empty}>{t('empty')}</p>
      ) : (
        <ul className={styles.tree} aria-label={t('treeLabel')}>
          {tree.map((node) => (
            <CategoryNode
              key={node.code}
              node={node}
              classificationCopy={classificationCopy as unknown as Translator}
              activeLabel={common('active')}
              inactiveLabel={common('inactive')}
              translateLabel={translateLabel}
            />
          ))}
        </ul>
      )}

      <CreateCategoryForm locale={locale} parentOptions={parentOptions} />
    </div>
  );
}

function CategoryNode({
  node,
  classificationCopy,
  activeLabel,
  inactiveLabel,
  translateLabel,
}: {
  node: AdminCategoryNode;
  classificationCopy: Translator;
  activeLabel: string;
  inactiveLabel: string;
  translateLabel: string;
}) {
  const [showTranslate, setShowTranslate] = useState(false);
  const id = node.id;

  return (
    <li
      className={`${styles.treeNode}${node.active ? '' : ` ${styles.inactive}`}`}
      data-category-code={node.code}
      data-category-active={node.active ? 'true' : 'false'}
    >
      <div className={styles.treeRow}>
        <span className={styles.rowName}>{node.name}</span>
        <span className={`${styles.badge} ${styles.code}`}>{node.code}</span>
        <span className={styles.badge}>
          {classificationCopy.has(node.classification)
            ? classificationCopy(node.classification)
            : node.classification}
        </span>
        <span
          className={`${styles.badge} ${node.active ? styles.badgeSuccess : styles.badgeNeutral}`}
        >
          {node.active ? activeLabel : inactiveLabel}
        </span>
        <Button
          variant="quiet"
          size="sm"
          type="button"
          onClick={() => setShowTranslate((open) => !open)}
        >
          {translateLabel}
        </Button>
      </div>

      {showTranslate ? (
        <TranslationEditorPanel basePath="/api/taxonomy/categories" resourceId={id} supportsApprove />
      ) : null}

      {node.children.length > 0 ? (
        <ul className={styles.treeChildren}>
          {node.children.map((child) => (
            <CategoryNode
              key={child.code}
              node={child}
              classificationCopy={classificationCopy}
              activeLabel={activeLabel}
              inactiveLabel={inactiveLabel}
              translateLabel={translateLabel}
            />
          ))}
        </ul>
      ) : null}
    </li>
  );
}

function CreateCategoryForm({
  locale,
  parentOptions,
}: {
  locale: string;
  parentOptions: { code: string; name: string; depth: number }[];
}) {
  const t = useTranslations('taxonomyAdmin.categories.create');
  const classificationCopy = useTranslations('taxonomyAdmin.classification');
  const categories = useTranslations('taxonomyAdmin.categories');
  const validate = useTranslations('taxonomyAdmin.validation') as unknown as TranslateFn;
  const common = useTranslations('taxonomyAdmin.common');
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const schema = useMemo(() => createCategorySchema(validate), [validate]);
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateCategoryValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      code: '',
      parentCode: '',
      classification: 'FRAME',
      originalLocale: locale,
      name: '',
      sortOrder: '',
    },
  });

  const [formError, setFormError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const nameValue = watch('name');

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setNotice(null);
    const body: CreateCategoryRequest = {
      code: values.code.trim(),
      classification: values.classification,
      originalLocale: values.originalLocale,
      name: values.name.trim(),
    };
    if (values.parentCode) {
      body.parentCode = values.parentCode;
    }
    if (values.sortOrder) {
      body.sortOrder = Number(values.sortOrder);
    }
    try {
      const response = await sendJson('/api/taxonomy/categories', 'POST', body);
      if (response.ok) {
        setNotice(t('created'));
        reset({
          code: '',
          parentCode: '',
          classification: 'FRAME',
          originalLocale: locale,
          name: '',
          sortOrder: '',
        });
        router.refresh();
        return;
      }
      setFormError(translateTaxonomyError(await readBffError(response), serverErrors));
    } catch {
      setFormError(serverErrors('unexpected'));
    }
  });

  return (
    <form className={styles.form} method="post" onSubmit={onSubmit} noValidate aria-label={t('title')}>
      <h3 className={styles.subheading}>{t('title')}</h3>
      {formError ? <Alert tone="danger">{formError}</Alert> : null}
      {notice ? <Alert tone="success">{notice}</Alert> : null}

      <div className={styles.grid}>
        <FormField label={t('codeLabel')} htmlFor="cat-code" error={errors.code?.message} hint={t('codeHint')}>
          <input id="cat-code" className={styles.input} type="text" {...register('code')} />
        </FormField>
        <FormField label={t('nameLabel')} htmlFor="cat-name" error={errors.name?.message}>
          <input id="cat-name" className={styles.input} type="text" {...register('name')} />
        </FormField>
        <FormField label={t('parentLabel')} htmlFor="cat-parent" error={errors.parentCode?.message}>
          <select id="cat-parent" className={styles.select} {...register('parentCode')}>
            <option value="">{categories('rootLabel')}</option>
            {parentOptions.map((option) => (
              <option key={option.code} value={option.code}>
                {`${'—'.repeat(option.depth)} ${option.name} (${option.code})`}
              </option>
            ))}
          </select>
        </FormField>
        <FormField label={t('classificationLabel')} htmlFor="cat-classification" error={errors.classification?.message}>
          <select id="cat-classification" className={styles.select} {...register('classification')}>
            {CATEGORY_CLASSIFICATIONS.map((code) => (
              <option key={code} value={code}>
                {classificationCopy(code)}
              </option>
            ))}
          </select>
        </FormField>
        <FormField label={t('originalLocaleLabel')} htmlFor="cat-locale" error={errors.originalLocale?.message}>
          <select id="cat-locale" className={styles.select} {...register('originalLocale')}>
            {LOCALES.map((code) => (
              <option key={code} value={code}>
                {LOCALE_ENDONYMS[code]}
              </option>
            ))}
          </select>
        </FormField>
        <FormField label={t('sortOrderLabel')} htmlFor="cat-sort" error={errors.sortOrder?.message}>
          <input id="cat-sort" className={styles.input} type="text" inputMode="numeric" {...register('sortOrder')} />
        </FormField>
      </div>

      {nameValue ? (
        <p className={styles.slugHint}>{common('slugPreview', { slug: previewSlug(nameValue) })}</p>
      ) : null}

      <div className={styles.actions}>
        <Button variant="primary" size="md" type="submit" loading={isSubmitting}>
          {t('submit')}
        </Button>
      </div>
    </form>
  );
}
