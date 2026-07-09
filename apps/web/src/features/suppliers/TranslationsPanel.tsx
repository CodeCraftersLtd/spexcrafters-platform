'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useState } from 'react';

import type { ProfileTranslation } from '@spexcrafters/api-client';
import { Alert, Button } from '@spexcrafters/ui';

import { LOCALE_ENDONYMS, type SupportedLocale } from '@/i18n/locales';
import type { Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';
import { translateSupplierError } from '@/features/suppliers/errors';
import {
  needsStaleWarning,
  orderTranslations,
  translationIndicator,
} from '@/features/suppliers/translations';
import { TranslationEditor } from '@/features/suppliers/TranslationEditor';
import type { TranslationValues } from '@/features/suppliers/schemas';

import styles from './supplier.module.css';

interface TranslationsPanelProps {
  supplierId: string;
  originalLocale: string;
  translations: ProfileTranslation[];
  availableLocales: SupportedLocale[];
  canUpdate: boolean;
}

function toValues(row: ProfileTranslation | undefined): TranslationValues {
  return {
    tradingName: row?.tradingName ?? '',
    companyDescription: row?.companyDescription ?? '',
    productionCapabilityDescription: row?.productionCapabilityDescription ?? '',
    oemDescription: row?.oemDescription ?? '',
    odmDescription: row?.odmDescription ?? '',
    privateLabelDescription: row?.privateLabelDescription ?? '',
    qualityControlDescription: row?.qualityControlDescription ?? '',
    exportMarketDescription: row?.exportMarketDescription ?? '',
  };
}

export function TranslationsPanel({
  supplierId,
  originalLocale,
  translations,
  availableLocales,
  canUpdate,
}: TranslationsPanelProps) {
  const t = useTranslations('suppliers.translations');
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const [editingLocale, setEditingLocale] = useState<string | null>(null);
  const [addLocale, setAddLocale] = useState('');
  const [listError, setListError] = useState<string | null>(null);
  const [busyLocale, setBusyLocale] = useState<string | null>(null);

  const rows = orderTranslations(translations).filter((row) => !row.original);
  const presentLocales = new Set(translations.map((row) => row.locale));
  const addableLocales = availableLocales.filter(
    (code) => code !== originalLocale && !presentLocales.has(code),
  );

  async function decide(locale: string, action: 'approve' | 'reject') {
    setListError(null);
    setBusyLocale(locale);
    try {
      let response: Response;
      try {
        response = await sendJson(
          `/api/supplier/${encodeURIComponent(supplierId)}/profile/translations/${encodeURIComponent(locale)}/${action}`,
          'POST',
        );
      } catch {
        setListError(serverErrors('unexpected'));
        return;
      }
      if (response.ok) {
        router.refresh();
        return;
      }
      setListError(translateSupplierError(await readBffError(response), serverErrors));
    } finally {
      setBusyLocale(null);
    }
  }

  return (
    <section className={styles.stack} aria-label={t('title')}>
      <h3 className={styles.subheading}>{t('title')}</h3>
      <p className={styles.intro}>{t('intro')}</p>
      {listError ? <Alert tone="danger">{listError}</Alert> : null}

      {rows.length === 0 ? (
        <p className={styles.empty}>{t('empty')}</p>
      ) : (
        <ul className={styles.list}>
          {rows.map((row) => {
            const indicator = translationIndicator(row);
            const localeLabel = LOCALE_ENDONYMS[row.locale as SupportedLocale] ?? row.locale;
            return (
              <li key={row.locale} className={styles.stack}>
                <div className={styles.row}>
                  <div className={styles.rowMain}>
                    <span className={styles.rowName}>{localeLabel}</span>
                    <span className={styles.rowMeta}>
                      {t('statusLabel')}: {t(`state.${row.translationStatus}`)}
                    </span>
                  </div>
                  <div className={styles.rowActions}>
                    <span className={styles.badge}>{t(`indicator.${indicator}`)}</span>
                    {canUpdate ? (
                      <>
                        <Button variant="secondary" size="sm" type="button" onClick={() => setEditingLocale(editingLocale === row.locale ? null : row.locale)}>
                          {t('edit')}
                        </Button>
                        <Button variant="secondary" size="sm" type="button" loading={busyLocale === row.locale} onClick={() => void decide(row.locale, 'approve')}>
                          {t('approve')}
                        </Button>
                        <Button variant="secondary" size="sm" type="button" loading={busyLocale === row.locale} onClick={() => void decide(row.locale, 'reject')}>
                          {t('reject')}
                        </Button>
                      </>
                    ) : null}
                  </div>
                </div>
                {needsStaleWarning(row) ? <Alert tone="warning">{t('staleWarning')}</Alert> : null}
                {canUpdate && editingLocale === row.locale ? (
                  <TranslationEditor
                    supplierId={supplierId}
                    locale={row.locale}
                    isOriginal={false}
                    defaults={toValues(row)}
                    onSaved={() => setEditingLocale(null)}
                  />
                ) : null}
              </li>
            );
          })}
        </ul>
      )}

      {canUpdate && addableLocales.length > 0 ? (
        <div className={styles.stack}>
          <h4 className={styles.subheading}>{t('addTitle')}</h4>
          <div className={styles.actions}>
            <label htmlFor="add-translation-locale">{t('localeLabel')}</label>
            <select
              id="add-translation-locale"
              className={styles.select}
              value={addLocale}
              onChange={(event) => setAddLocale(event.target.value)}
            >
              <option value="">{t('localePlaceholder')}</option>
              {addableLocales.map((code) => (
                <option key={code} value={code}>
                  {LOCALE_ENDONYMS[code] ?? code}
                </option>
              ))}
            </select>
          </div>
          {addLocale ? (
            <TranslationEditor
              key={addLocale}
              supplierId={supplierId}
              locale={addLocale}
              isOriginal={false}
              defaults={toValues(undefined)}
              onSaved={() => setAddLocale('')}
            />
          ) : null}
        </div>
      ) : null}
    </section>
  );
}
