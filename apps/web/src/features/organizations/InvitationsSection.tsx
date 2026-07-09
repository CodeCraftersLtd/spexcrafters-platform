'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import type { Capability, InvitationResponse } from '@spexcrafters/api-client';
import { Alert, Button, FormField, Input } from '@spexcrafters/ui';

import type { SupportedLocale } from '@/i18n/locales';
import type { TranslateFn, Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';
import { invitableRoles } from '@/features/organizations/capabilities';
import { translateOrgError } from '@/features/organizations/org-errors';
import {
  inviteMemberSchema,
  type InviteMemberFormValues,
} from '@/features/organizations/schemas';

import styles from './org-components.module.css';

interface InvitationsSectionProps {
  locale: SupportedLocale;
  organizationId: string;
  invitations: InvitationResponse[];
  callerCapabilities: Capability[];
}

/**
 * Invitations list plus the invite form. Both the form and the revoke
 * controls are rendered only for callers holding organization.members.invite;
 * the ADMIN role option additionally requires organization.roles.manage
 * (capability model §2 — ADMIN actors invite MEMBER-role targets only).
 */
export function InvitationsSection({
  locale,
  organizationId,
  invitations,
  callerCapabilities,
}: InvitationsSectionProps) {
  const t = useTranslations('organizations.workspace.invitations');
  const status = useTranslations('organizations.workspace.invitations.status');
  const roles = useTranslations('organizations.roles');
  const serverErrors = useTranslations(
    'organizations.serverErrors',
  ) as unknown as Translator;

  const router = useRouter();
  const roleOptions = invitableRoles(callerCapabilities);
  const canInvite = roleOptions.length > 0;

  const [listError, setListError] = useState<string | null>(null);
  const [busyInvitationId, setBusyInvitationId] = useState<string | null>(null);

  const dateFormatter = useMemo(
    () => new Intl.DateTimeFormat(locale, { dateStyle: 'medium' }),
    [locale],
  );

  async function revoke(invitation: InvitationResponse) {
    setListError(null);
    setBusyInvitationId(invitation.id);
    try {
      let response: Response;
      try {
        response = await sendJson(
          `/api/orgs/${encodeURIComponent(organizationId)}/invitations/${encodeURIComponent(invitation.id)}/revoke`,
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
      setListError(translateOrgError(await readBffError(response), serverErrors));
    } finally {
      setBusyInvitationId(null);
    }
  }

  return (
    <section className={styles.stack} aria-label={t('title')}>
      <h2 className={styles.subheading}>{t('title')}</h2>
      {listError ? <Alert tone="danger">{listError}</Alert> : null}

      {invitations.length === 0 ? (
        <p className={styles.empty}>{t('empty')}</p>
      ) : (
        <ul className={styles.list} aria-label={t('listLabel')}>
          {invitations.map((invitation) => (
            <li key={invitation.id} className={styles.row}>
              <div className={styles.rowMain}>
                <span className={styles.rowName}>{invitation.email}</span>
                <span className={styles.rowMeta}>
                  {roles(invitation.role)} ·{' '}
                  {t('expires', {
                    date: dateFormatter.format(new Date(invitation.expiresAt)),
                  })}
                </span>
              </div>
              <div className={styles.rowActions}>
                <span className={styles.badge}>{status(invitation.status)}</span>
                {canInvite && invitation.status === 'PENDING' ? (
                  <Button
                    variant="secondary"
                    size="sm"
                    type="button"
                    loading={busyInvitationId === invitation.id}
                    aria-label={t('revokeInvitationLabel', { email: invitation.email })}
                    onClick={() => void revoke(invitation)}
                  >
                    {t('revoke')}
                  </Button>
                ) : null}
              </div>
            </li>
          ))}
        </ul>
      )}

      {canInvite ? (
        <InviteForm
          organizationId={organizationId}
          roleOptions={roleOptions}
          onInvited={() => router.refresh()}
        />
      ) : null}
    </section>
  );
}

interface InviteFormProps {
  organizationId: string;
  roleOptions: ReadonlyArray<'ADMIN' | 'MEMBER'>;
  onInvited: () => void;
}

function InviteForm({ organizationId, roleOptions, onInvited }: InviteFormProps) {
  const t = useTranslations('organizations.workspace.invitations');
  const roles = useTranslations('organizations.roles');
  const validate = useTranslations('organizations.validation') as unknown as TranslateFn;
  const serverErrors = useTranslations(
    'organizations.serverErrors',
  ) as unknown as Translator;

  const schema = useMemo(() => inviteMemberSchema(validate), [validate]);
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<InviteMemberFormValues>({
    resolver: zodResolver(schema),
    defaultValues: { role: 'MEMBER' },
  });

  const [formError, setFormError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setSent(false);
    let response: Response;
    try {
      response = await sendJson(
        `/api/orgs/${encodeURIComponent(organizationId)}/invitations`,
        'POST',
        values,
      );
    } catch {
      setFormError(serverErrors('unexpected'));
      return;
    }

    if (response.ok) {
      reset({ email: '', role: values.role });
      setSent(true);
      onInvited();
      return;
    }

    const error = await readBffError(response);
    const emailError = error.fields?.email;
    if (emailError) {
      setError('email', {
        type: 'server',
        message: translateOrgError(emailError, serverErrors),
      });
      return;
    }
    setFormError(translateOrgError(error, serverErrors));
  });

  return (
    <form
      className={styles.inviteForm}
      method="post"
      onSubmit={onSubmit}
      noValidate
      aria-label={t('inviteTitle')}
    >
      <h3 className={styles.subheading}>{t('inviteTitle')}</h3>
      {formError ? <Alert tone="danger">{formError}</Alert> : null}
      {sent ? <Alert tone="success">{t('sent')}</Alert> : null}

      <div className={styles.inviteFields}>
        <FormField
          label={t('emailLabel')}
          htmlFor="invite-email"
          error={errors.email?.message}
        >
          <Input
            id="invite-email"
            type="email"
            autoComplete="off"
            invalid={Boolean(errors.email)}
            {...register('email')}
          />
        </FormField>

        <FormField
          label={t('roleLabel')}
          htmlFor="invite-role"
          error={errors.role?.message}
        >
          <select id="invite-role" className={styles.select} {...register('role')}>
            {roleOptions.map((role) => (
              <option key={role} value={role}>
                {roles(role)}
              </option>
            ))}
          </select>
        </FormField>
      </div>

      <div className={styles.actions}>
        <Button variant="primary" size="md" type="submit" loading={isSubmitting}>
          {t('submit')}
        </Button>
      </div>
    </form>
  );
}
