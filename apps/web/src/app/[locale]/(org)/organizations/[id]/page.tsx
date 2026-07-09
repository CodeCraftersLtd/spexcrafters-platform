import type { Metadata } from 'next';
import Link from 'next/link';
import { redirect } from 'next/navigation';
import { NextIntlClientProvider } from 'next-intl';
import { getTranslations, setRequestLocale } from 'next-intl/server';

import {
  ApiProblemError,
  type InvitationResponse,
  type MemberResponse,
  type OrganizationResponse,
} from '@spexcrafters/api-client';
import { Alert } from '@spexcrafters/ui';

import { InvitationsSection } from '@/features/organizations/InvitationsSection';
import { MembersSection } from '@/features/organizations/MembersSection';
import { OrgProfileForm } from '@/features/organizations/OrgProfileForm';
import { hasCapability } from '@/features/organizations/capabilities';
import { DEFAULT_LOCALE, isSupportedLocale, type SupportedLocale } from '@/i18n/locales';
import { pickMessages } from '@/i18n/pick';
import { createServerApiClient } from '@/lib/server-api';
import { getSession } from '@/lib/session';

import styles from './page.module.css';

interface OrganizationWorkspacePageProps {
  params: Promise<{ locale: string; id: string }>;
}

export async function generateMetadata({
  params,
}: OrganizationWorkspacePageProps): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'organizations.workspace' });
  return { title: t('metaTitle') };
}

export default async function OrganizationWorkspacePage({
  params,
}: OrganizationWorkspacePageProps) {
  const { locale: raw, id } = await params;
  const locale: SupportedLocale = isSupportedLocale(raw) ? raw : DEFAULT_LOCALE;
  setRequestLocale(locale);

  const session = await getSession();
  if (!session) {
    redirect(
      `/${locale}/auth/login?returnTo=${encodeURIComponent(`/${locale}/organizations/${id}`)}`,
    );
  }

  const [copy, types, roles] = await Promise.all([
    getTranslations({ locale, namespace: 'organizations.workspace' }),
    getTranslations({ locale, namespace: 'organizations.types' }),
    getTranslations({ locale, namespace: 'organizations.roles' }),
  ]);

  const api = createServerApiClient(session.accessToken);

  let organization: OrganizationResponse;
  try {
    organization = await api.getOrganization(id);
  } catch (error) {
    if (error instanceof ApiProblemError) {
      if (error.problem.status === 401) {
        redirect(
          `/${locale}/auth/login?returnTo=${encodeURIComponent(`/${locale}/organizations/${id}`)}`,
        );
      }
      if (error.problem.status === 404) {
        // Concealment: non-members and unknown ids get the same state.
        return (
          <section className={styles.notFound} aria-labelledby="org-not-found-title">
            <h1 id="org-not-found-title" className={styles.notFoundTitle}>
              {copy('notFound.title')}
            </h1>
            <p className={styles.notFoundBody}>{copy('notFound.body')}</p>
            <Link className={styles.notFoundLink} href={`/${locale}/organizations`}>
              {copy('notFound.backToList')}
            </Link>
          </section>
        );
      }
    }
    return (
      <section className={styles.page}>
        <Alert tone="danger">{copy('loadError')}</Alert>
      </section>
    );
  }

  // Reads gated by organization.members.read (every role holds it today; a
  // 403 here means the capability model changed — degrade to a section-level
  // error rather than failing the whole workspace).
  const canReadMembers = hasCapability(
    organization.callerCapabilities,
    'organization.members.read',
  );
  let members: MemberResponse[] | null = null;
  let invitations: InvitationResponse[] | null = null;
  if (canReadMembers) {
    [members, invitations] = await Promise.all([
      api.listMembers(id).catch(() => null),
      api.listInvitations(id).catch(() => null),
    ]);
  }

  const dateFormatter = new Intl.DateTimeFormat(locale, { dateStyle: 'medium' });
  const countryNames = new Intl.DisplayNames([locale], { type: 'region' });
  const canUpdate = hasCapability(
    organization.callerCapabilities,
    'organization.update',
  );

  const messages = await pickMessages(['organizations', 'common']);

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>{organization.name}</h1>
        <p className={styles.badges}>
          <span className={styles.badge}>{types(organization.type)}</span>
          <span className={`${styles.badge} ${styles.badgeRole}`}>
            {`${copy('yourRoleLabel')}: ${roles(organization.callerRole)}`}
          </span>
        </p>
        <dl className={styles.summary} aria-label={copy('summaryLabel')}>
          <div className={styles.summaryItem}>
            <dt className={styles.summaryLabel}>{copy('typeLabel')}</dt>
            <dd className={styles.summaryValue}>{types(organization.type)}</dd>
          </div>
          {organization.country ? (
            <div className={styles.summaryItem}>
              <dt className={styles.summaryLabel}>{copy('countryLabel')}</dt>
              <dd className={styles.summaryValue}>
                {countryNames.of(organization.country) ?? organization.country}
              </dd>
            </div>
          ) : null}
          <div className={styles.summaryItem}>
            <dt className={styles.summaryLabel}>{copy('createdLabel')}</dt>
            <dd className={styles.summaryValue}>
              {dateFormatter.format(new Date(organization.createdAt))}
            </dd>
          </div>
        </dl>
      </header>

      <NextIntlClientProvider locale={locale} messages={messages}>
        {!canReadMembers ? null : members === null ? (
          <Alert tone="danger">{copy('loadError')}</Alert>
        ) : (
          <MembersSection
            locale={locale}
            organizationId={organization.id}
            members={members}
            callerRole={organization.callerRole}
            callerCapabilities={organization.callerCapabilities}
            currentUserId={session.user.id}
          />
        )}

        {!canReadMembers ? null : invitations === null ? (
          <Alert tone="danger">{copy('loadError')}</Alert>
        ) : (
          <InvitationsSection
            locale={locale}
            organizationId={organization.id}
            invitations={invitations}
            callerCapabilities={organization.callerCapabilities}
          />
        )}

        {canUpdate ? (
          <OrgProfileForm
            organizationId={organization.id}
            name={organization.name}
            country={organization.country}
            version={organization.version}
          />
        ) : null}
      </NextIntlClientProvider>
    </div>
  );
}
