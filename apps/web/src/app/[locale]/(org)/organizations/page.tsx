import type { Metadata } from 'next';
import Link from 'next/link';
import { redirect } from 'next/navigation';

import { ApiProblemError, type MyMembership } from '@spexcrafters/api-client';
import { Alert } from '@spexcrafters/ui';

import {
  defaultLocale,
  getDictionary,
  interpolate,
  isLocale,
  type Locale,
} from '@/lib/i18n';
import { createServerApiClient } from '@/lib/server-api';
import { getSession } from '@/lib/session';

import styles from './page.module.css';

interface OrganizationsPageProps {
  params: Promise<{ locale: string }>;
}

export async function generateMetadata({
  params,
}: OrganizationsPageProps): Promise<Metadata> {
  const { locale } = await params;
  return { title: getDictionary(locale).organizations.metaTitle };
}

export default async function OrganizationsPage({ params }: OrganizationsPageProps) {
  const { locale: raw } = await params;
  const locale: Locale = isLocale(raw) ? raw : defaultLocale;
  const dict = getDictionary(locale);
  const copy = dict.organizations.list;

  const session = await getSession();
  if (!session) {
    redirect(
      `/${locale}/auth/login?returnTo=${encodeURIComponent(`/${locale}/organizations`)}`,
    );
  }

  let memberships: MyMembership[] | null = null;
  try {
    memberships = await createServerApiClient(session.accessToken).listMyOrganizations();
  } catch (error) {
    if (error instanceof ApiProblemError && error.problem.status === 401) {
      // Access token expired mid-session; a fresh login restores it.
      redirect(
        `/${locale}/auth/login?returnTo=${encodeURIComponent(`/${locale}/organizations`)}`,
      );
    }
    memberships = null;
  }

  const dateFormatter = new Intl.DateTimeFormat(locale, { dateStyle: 'medium' });

  return (
    <section className={styles.page}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>{copy.title}</h1>
          <p className={styles.intro}>{copy.intro}</p>
        </div>
        <Link className={styles.createLink} href={`/${locale}/organizations/new`}>
          {copy.createCta}
        </Link>
      </div>

      {memberships === null ? (
        <Alert tone="danger">{copy.loadError}</Alert>
      ) : memberships.length === 0 ? (
        <div className={styles.empty}>
          <h2 className={styles.emptyTitle}>{copy.empty.title}</h2>
          <p className={styles.emptyBody}>{copy.empty.body}</p>
          <Link className={styles.createLink} href={`/${locale}/organizations/new`}>
            {copy.empty.cta}
          </Link>
        </div>
      ) : (
        <ul className={styles.grid}>
          {memberships.map((membership) => (
            <li key={membership.membershipId} className={styles.card}>
              <h2 className={styles.cardName}>
                <Link
                  href={`/${locale}/organizations/${membership.organizationId}`}
                  aria-label={interpolate(copy.open, {
                    name: membership.organizationName,
                  })}
                >
                  {membership.organizationName}
                </Link>
              </h2>
              <p className={styles.cardMeta}>
                <span className={styles.badge}>
                  {dict.organizations.types[membership.organizationType]}
                </span>
                <span className={styles.badge}>
                  {dict.organizations.roles[membership.role]}
                </span>
                <span>
                  {interpolate(copy.joined, {
                    date: dateFormatter.format(new Date(membership.joinedAt)),
                  })}
                </span>
              </p>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
