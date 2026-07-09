'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useState } from 'react';

import type {
  Capability,
  MemberResponse,
  OrganizationRole,
} from '@spexcrafters/api-client';
import { Alert, Button } from '@spexcrafters/ui';

import type { SupportedLocale } from '@/i18n/locales';
import type { Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';
import {
  canChangeRole,
  canRemoveMember,
} from '@/features/organizations/capabilities';
import { translateOrgError } from '@/features/organizations/org-errors';

import styles from './org-components.module.css';

interface MembersSectionProps {
  locale: SupportedLocale;
  organizationId: string;
  members: MemberResponse[];
  callerRole: OrganizationRole;
  callerCapabilities: Capability[];
  currentUserId: string;
}

const ALL_ROLES: OrganizationRole[] = ['OWNER', 'ADMIN', 'MEMBER'];

/**
 * Members list with capability-aware controls. Mutations go through the BFF
 * proxies; on success the surrounding Server Component is re-rendered via
 * router.refresh() (no optimistic state).
 */
export function MembersSection({
  locale,
  organizationId,
  members,
  callerRole,
  callerCapabilities,
  currentUserId,
}: MembersSectionProps) {
  const t = useTranslations('organizations.workspace.members');
  const roles = useTranslations('organizations.roles');
  const serverErrors = useTranslations(
    'organizations.serverErrors',
  ) as unknown as Translator;

  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [busyMembershipId, setBusyMembershipId] = useState<string | null>(null);

  async function removeMember(member: MemberResponse, isSelf: boolean) {
    setError(null);
    setBusyMembershipId(member.membershipId);
    try {
      let response: Response;
      try {
        response = await sendJson(
          `/api/orgs/${encodeURIComponent(organizationId)}/members/${encodeURIComponent(member.membershipId)}`,
          'DELETE',
        );
      } catch {
        setError(serverErrors('unexpected'));
        return;
      }
      if (response.ok) {
        if (isSelf) {
          // After leaving, the workspace would 404 (concealment) — go home.
          window.location.assign(`/${locale}/organizations`);
          return;
        }
        router.refresh();
        return;
      }
      setError(translateOrgError(await readBffError(response), serverErrors));
    } finally {
      setBusyMembershipId(null);
    }
  }

  async function changeRole(member: MemberResponse, role: OrganizationRole) {
    setError(null);
    setBusyMembershipId(member.membershipId);
    try {
      let response: Response;
      try {
        response = await sendJson(
          `/api/orgs/${encodeURIComponent(organizationId)}/members/${encodeURIComponent(member.membershipId)}/role`,
          'PUT',
          { role },
        );
      } catch {
        setError(serverErrors('unexpected'));
        return;
      }
      if (response.ok) {
        router.refresh();
        return;
      }
      setError(translateOrgError(await readBffError(response), serverErrors));
    } finally {
      setBusyMembershipId(null);
    }
  }

  return (
    <section className={styles.stack} aria-label={t('title')}>
      <h2 className={styles.subheading}>{t('title')}</h2>
      {error ? <Alert tone="danger">{error}</Alert> : null}
      {members.length === 0 ? (
        <p className={styles.empty}>{t('empty')}</p>
      ) : (
        <ul className={styles.list} aria-label={t('listLabel')}>
          {members.map((member) => {
            const isSelf = member.userId === currentUserId;
            const busy = busyMembershipId === member.membershipId;
            const showRoleSelect = canChangeRole(callerCapabilities, isSelf);
            const showRemove = canRemoveMember(
              callerRole,
              callerCapabilities,
              member.role,
              isSelf,
            );
            return (
              <li key={member.membershipId} className={styles.row}>
                <div className={styles.rowMain}>
                  <span className={styles.rowName}>
                    {member.displayName}
                    {isSelf ? ` (${t('youLabel')})` : ''}
                  </span>
                  <span className={styles.rowMeta}>{member.email}</span>
                </div>
                <div className={styles.rowActions}>
                  {showRoleSelect ? (
                    <>
                      <label
                        className="sc-visually-hidden"
                        htmlFor={`member-role-${member.membershipId}`}
                      >
                        {t('roleSelectLabel', { name: member.displayName })}
                      </label>
                      <select
                        id={`member-role-${member.membershipId}`}
                        className={`${styles.select} ${styles.inlineSelect}`}
                        value={member.role}
                        disabled={busy}
                        onChange={(event) =>
                          void changeRole(
                            member,
                            event.target.value as OrganizationRole,
                          )
                        }
                      >
                        {ALL_ROLES.map((role) => (
                          <option key={role} value={role}>
                            {roles(role)}
                          </option>
                        ))}
                      </select>
                    </>
                  ) : (
                    <span
                      className={`${styles.badge} ${member.role === 'OWNER' ? styles.badgeOwner : ''}`}
                    >
                      {roles(member.role)}
                    </span>
                  )}
                  {showRemove ? (
                    <Button
                      variant="destructive"
                      size="sm"
                      type="button"
                      loading={busy}
                      aria-label={
                        isSelf
                          ? t('leave')
                          : t('removeMemberLabel', { name: member.displayName })
                      }
                      onClick={() => void removeMember(member, isSelf)}
                    >
                      {isSelf ? t('leave') : t('remove')}
                    </Button>
                  ) : null}
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
