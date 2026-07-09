import type { Metadata } from 'next';
import { redirect } from 'next/navigation';
import { NextIntlClientProvider } from 'next-intl';
import { getTranslations, setRequestLocale } from 'next-intl/server';

import {
  ApiProblemError,
  type Evidence,
  type ReviewRequest,
  type SupplierApplication,
  type SupplierProfile,
  type VerificationStatus,
} from '@spexcrafters/api-client';
import { Alert } from '@spexcrafters/ui';

import { ChangeRequestsSection } from '@/features/suppliers/ChangeRequestsSection';
import { DraftForm } from '@/features/suppliers/DraftForm';
import { EvidenceSection } from '@/features/suppliers/EvidenceSection';
import { FacilitiesSection } from '@/features/suppliers/FacilitiesSection';
import { ReviewSubmit } from '@/features/suppliers/ReviewSubmit';
import { TranslationEditor } from '@/features/suppliers/TranslationEditor';
import { TranslationsPanel } from '@/features/suppliers/TranslationsPanel';
import {
  canSubmit as canSubmitFn,
  canWithdraw as canWithdrawFn,
  hasSupplierCapability,
  isDraftEditable,
} from '@/features/suppliers/capabilities';
import type { CompanyInfoValues, TranslationValues } from '@/features/suppliers/schemas';
import {
  completenessChecks,
  originalTranslation,
  statusTone,
} from '@/features/suppliers/status';
import {
  CAPABILITY_CODES,
  COMPANY_TYPE_CODES,
  EMPLOYEE_RANGE_CODES,
  SUPPLIER_TYPE_CODES,
} from '@/features/suppliers/taxonomy';
import {
  DEFAULT_LOCALE,
  LOCALES,
  LOCALE_ENDONYMS,
  isSupportedLocale,
  type SupportedLocale,
} from '@/i18n/locales';
import { pickMessages } from '@/i18n/pick';
import { createServerApiClient } from '@/lib/server-api';
import { getSession } from '@/lib/session';

import styles from './page.module.css';

interface WorkspacePageProps {
  params: Promise<{ locale: string; applicationId: string }>;
}

export async function generateMetadata({
  params,
}: WorkspacePageProps): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'suppliers.application' });
  return { title: t('metaTitle') };
}

function asEnum<T extends readonly string[]>(
  value: string | null | undefined,
  codes: T,
): T[number] | '' {
  return value && (codes as readonly string[]).includes(value)
    ? (value as T[number])
    : '';
}

function knownOnly<T extends readonly string[]>(
  values: readonly string[],
  codes: T,
): T[number][] {
  return values.filter((code): code is T[number] =>
    (codes as readonly string[]).includes(code),
  );
}

function toTranslationValues(
  row: SupplierProfile['translations'][number] | undefined,
): TranslationValues {
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

export default async function SupplierApplicationWorkspacePage({
  params,
}: WorkspacePageProps) {
  const { locale: raw, applicationId } = await params;
  const locale: SupportedLocale = isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
  setRequestLocale(locale);

  const returnTo = encodeURIComponent(
    `/${locale}/supplier/applications/${applicationId}`,
  );

  const session = await getSession();
  if (!session) {
    redirect(`/${locale}/auth/login?returnTo=${returnTo}`);
  }

  const [t, s, e, statusCopy, verificationCopy, taxonomy] = await Promise.all([
    getTranslations({ locale, namespace: 'suppliers.application' }),
    getTranslations({ locale, namespace: 'suppliers' }),
    getTranslations({ locale, namespace: 'evidence' }),
    getTranslations({ locale, namespace: 'suppliers.status' }),
    getTranslations({ locale, namespace: 'verification.status' }),
    getTranslations({ locale, namespace: 'taxonomy' }),
  ]);

  const api = createServerApiClient(session.accessToken);

  let application: SupplierApplication;
  try {
    application = await api.getSupplierApplication(applicationId);
  } catch (error) {
    if (error instanceof ApiProblemError) {
      if (error.problem.status === 401) {
        redirect(`/${locale}/auth/login?returnTo=${returnTo}`);
      }
      if (error.problem.status === 404) {
        return (
          <section className={styles.notFound}>
            <h1 className={styles.title}>{t('notFound.title')}</h1>
            <p className={styles.intro}>{t('notFound.body')}</p>
          </section>
        );
      }
      if (error.problem.status === 403) {
        return (
          <section className={styles.notFound}>
            <h1 className={styles.title}>{t('forbidden.title')}</h1>
            <p className={styles.intro}>{t('forbidden.body')}</p>
          </section>
        );
      }
    }
    return (
      <section className={styles.page}>
        <Alert tone="danger">{t('loadError')}</Alert>
      </section>
    );
  }

  const caps = application.callerCapabilities;
  const canUpdate = hasSupplierCapability(caps, 'supplier.update');
  const editable = isDraftEditable(application.status) && canUpdate;
  const canReadEvidence = hasSupplierCapability(caps, 'supplier.evidence.read');
  const canUpload = hasSupplierCapability(caps, 'supplier.evidence.upload');
  const canDelete = hasSupplierCapability(caps, 'supplier.evidence.delete');
  const canReadVerification = hasSupplierCapability(caps, 'supplier.verification.read');

  const [profile, evidence, verification, changeRequests] = await Promise.all([
    api.getSupplierProfile(application.supplierId).catch((): SupplierProfile | null => null),
    canReadEvidence
      ? api.listEvidence(application.supplierId).catch((): Evidence[] => [])
      : Promise.resolve<Evidence[]>([]),
    canReadVerification
      ? api
          .getVerificationStatus(application.supplierId)
          .catch((): VerificationStatus | null => null)
      : Promise.resolve<VerificationStatus | null>(null),
    api.listChangeRequests(applicationId).catch((): ReviewRequest[] => []),
  ]);

  if (!profile) {
    return (
      <section className={styles.page}>
        <Alert tone="danger">{t('loadError')}</Alert>
      </section>
    );
  }

  const draftDefaults: CompanyInfoValues = {
    legalName: profile.legalName ?? '',
    registeredLegalNameTranslated: profile.registeredLegalNameTranslated ?? '',
    tradingName: profile.tradingName ?? '',
    registrationNumber: profile.registrationNumber ?? '',
    countryOfRegistration: profile.countryOfRegistration ?? '',
    registrationAuthority: profile.registrationAuthority ?? '',
    registrationDate: profile.registrationDate ?? '',
    companyTypeCode: asEnum(profile.companyTypeCode, COMPANY_TYPE_CODES),
    yearEstablished:
      profile.yearEstablished != null ? String(profile.yearEstablished) : '',
    employeeRange: asEnum(profile.employeeRange, EMPLOYEE_RANGE_CODES),
    website: profile.website ?? '',
    businessEmail: profile.businessEmail ?? '',
    businessPhone: profile.businessPhone ?? '',
    types: knownOnly(profile.types, SUPPLIER_TYPE_CODES),
    capabilities: knownOnly(
      profile.capabilities.map((c) => c.capabilityCode),
      CAPABILITY_CODES,
    ),
  };

  const original = originalTranslation(profile);
  const originalLocaleLabel =
    LOCALE_ENDONYMS[application.originalLocale as SupportedLocale] ??
    application.originalLocale;
  const checks = completenessChecks(profile, evidence.length);
  const dateFormatter = new Intl.DateTimeFormat(locale, { dateStyle: 'medium' });

  const messages = await pickMessages([
    'suppliers',
    'evidence',
    'taxonomy',
    'errors',
    'common',
    'accessibility',
  ]);

  const statusLabel = statusCopy.has(`state.${application.status}`)
    ? statusCopy(`state.${application.status}`)
    : statusCopy('unknown');

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>{t('title')}</h1>
        <span className={styles.statusBadge} data-tone={statusTone(application.status)}>
          {statusLabel}
        </span>
      </header>

      {/* Status (read-only) */}
      <section className={styles.section} aria-labelledby="status-heading">
        <h2 id="status-heading" className={styles.sectionTitle}>{statusCopy('title')}</h2>
        <dl>
          <dt className={styles.intro}>{statusCopy('statusLabel')}</dt>
          <dd>{statusLabel}</dd>
          {application.submittedAt ? (
            <dd>
              {statusCopy('submittedLabel', {
                date: dateFormatter.format(new Date(application.submittedAt)),
              })}
            </dd>
          ) : null}
          {application.decidedAt ? (
            <dd>
              {statusCopy('decidedLabel', {
                date: dateFormatter.format(new Date(application.decidedAt)),
              })}
            </dd>
          ) : null}
        </dl>
      </section>

      <NextIntlClientProvider locale={locale} messages={messages}>
        {/* Company info + optical profile */}
        {editable ? (
          <section className={styles.section} aria-labelledby="company-heading">
            <h2 id="company-heading" className={styles.sectionTitle}>{s('companyInfo.title')}</h2>
            <DraftForm
              applicationId={applicationId}
              version={application.version}
              defaults={draftDefaults}
            />
          </section>
        ) : null}

        {/* Original-language content */}
        {editable ? (
          <section className={styles.section} aria-labelledby="content-heading">
            <h2 id="content-heading" className={styles.sectionTitle}>{s('content.title')}</h2>
            <p className={styles.intro}>
              {s('content.originalLocaleNote', { language: originalLocaleLabel })}
            </p>
            <TranslationEditor
              supplierId={application.supplierId}
              locale={application.originalLocale}
              isOriginal
              defaults={toTranslationValues(original)}
            />
          </section>
        ) : null}

        {/* Translations */}
        <section className={styles.section} aria-labelledby="translations-heading">
          <h2 id="translations-heading" className={styles.sectionTitle}>{s('translations.title')}</h2>
          <TranslationsPanel
            supplierId={application.supplierId}
            originalLocale={application.originalLocale}
            translations={profile.translations}
            availableLocales={[...LOCALES]}
            canUpdate={editable}
          />
        </section>

        {/* Facilities */}
        <section className={styles.section} aria-labelledby="facilities-heading">
          <h2 id="facilities-heading" className={styles.sectionTitle}>{s('facilities.title')}</h2>
          <FacilitiesSection
            supplierId={application.supplierId}
            facilities={profile.facilities}
            canUpdate={editable}
          />
        </section>

        {/* Evidence */}
        {canReadEvidence ? (
          <section className={styles.section} aria-labelledby="evidence-heading">
            <h2 id="evidence-heading" className={styles.sectionTitle}>{e('title')}</h2>
            <EvidenceSection
              supplierId={application.supplierId}
              evidence={evidence}
              canUpload={canUpload}
              canDelete={canDelete}
            />
          </section>
        ) : null}

        {/* Change requests */}
        <section className={styles.section} aria-labelledby="changes-heading">
          <h2 id="changes-heading" className={styles.sectionTitle}>{s('changeRequests.title')}</h2>
          <ChangeRequestsSection
            locale={locale}
            applicationId={applicationId}
            changeRequests={changeRequests}
            canRespond={canUpdate && application.status === 'CHANGES_REQUESTED'}
          />
        </section>

        {/* Review + submit */}
        <section className={styles.section} aria-labelledby="review-heading">
          <h2 id="review-heading" className={styles.sectionTitle}>{s('review.title')}</h2>
          <ReviewSubmit
            applicationId={applicationId}
            status={application.status}
            checks={checks}
            canSubmit={canSubmitFn(application.status, caps)}
            canWithdraw={canWithdrawFn(application.status, caps)}
          />
        </section>
      </NextIntlClientProvider>

      {/* Verification (scope-based, read-only) */}
      {canReadVerification && verification ? (
        <section className={styles.section} aria-labelledby="verification-heading">
          <h2 id="verification-heading" className={styles.sectionTitle}>
            {verificationCopy('title')}
          </h2>
          <p className={styles.intro}>{verificationCopy('disclaimer')}</p>
          {verification.scopes.length === 0 ? (
            <p className={styles.intro}>{verificationCopy('empty')}</p>
          ) : (
            <ul>
              {verification.scopes.map((scope) => (
                <li key={scope.scopeCode}>
                  <strong>
                    {taxonomy.has(`verificationScope.${scope.scopeCode}`)
                      ? taxonomy(`verificationScope.${scope.scopeCode}`)
                      : scope.scopeCode}
                  </strong>
                  {' — '}
                  {verificationCopy(`state.${scope.status}`)}
                  {' · '}
                  {verificationCopy('evidenceCount', { count: scope.evidenceIds.length })}
                </li>
              ))}
            </ul>
          )}
        </section>
      ) : null}
    </div>
  );
}
