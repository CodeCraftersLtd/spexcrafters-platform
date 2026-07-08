'use client';

import Link from 'next/link';
import { useEffect, useRef, useState } from 'react';

import type { MyMembership } from '@spexcrafters/api-client';
import { Alert } from '@spexcrafters/ui';

import type { Dictionary, Locale } from '@/lib/i18n';
import { sendJson } from '@/lib/csrf-client';
import { interpolate } from '@/lib/interpolate';
import { readBffError } from '@/features/auth/client-errors';
import { mapAcceptInvitationError } from '@/features/organizations/org-errors';

import styles from './org-components.module.css';

interface AcceptInvitationProps {
  locale: Locale;
  /** Token from the emailed link (?token=…); null when absent. */
  token: string | null;
  copy: Dictionary['invitations']['accept'];
}

type AcceptState =
  | { status: 'accepting' }
  | { status: 'success'; membership: MyMembership }
  | { status: 'error'; message: string; showOrganizationsLink: boolean };

/**
 * Posts the invitation token to the BFF on mount (same pattern as the
 * verify-email page) and renders the designed success / error states.
 */
export function AcceptInvitation({ locale, token, copy }: AcceptInvitationProps) {
  const [state, setState] = useState<AcceptState>(
    token
      ? { status: 'accepting' }
      : { status: 'error', message: copy.missingToken, showOrganizationsLink: false },
  );
  // Guards against React Strict Mode double-invocation so the single-use
  // token is posted exactly once per page view.
  const requested = useRef(false);

  useEffect(() => {
    if (!token || requested.current) {
      return;
    }
    requested.current = true;

    let cancelled = false;
    void (async () => {
      let response: Response;
      try {
        response = await sendJson('/api/invitations/accept', 'POST', { token });
      } catch {
        if (!cancelled) {
          setState({
            status: 'error',
            message: copy.genericError,
            showOrganizationsLink: false,
          });
        }
        return;
      }
      if (cancelled) {
        return;
      }
      if (response.ok) {
        const membership = (await response.json()) as MyMembership;
        setState({ status: 'success', membership });
        return;
      }
      const error = await readBffError(response);
      setState({
        status: 'error',
        ...mapAcceptInvitationError(response.status, error, copy),
      });
    })();

    return () => {
      cancelled = true;
    };
  }, [token, copy]);

  if (state.status === 'accepting') {
    return (
      <p role="status" aria-live="polite">
        {copy.accepting}
      </p>
    );
  }

  if (state.status === 'success') {
    const orgName = state.membership.organizationName;
    return (
      <div className={styles.stack}>
        <Alert tone="success" title={copy.successTitle}>
          {interpolate(copy.successBody, { org: orgName })}
        </Alert>
        <p>
          <Link
            href={`/${locale}/organizations/${state.membership.organizationId}`}
          >
            {interpolate(copy.goToOrganization, { org: orgName })}
          </Link>
        </p>
      </div>
    );
  }

  return (
    <div className={styles.stack}>
      <Alert tone="danger" title={copy.errorTitle}>
        {state.message}
      </Alert>
      {state.showOrganizationsLink ? (
        <p>
          <Link href={`/${locale}/organizations`}>{copy.goToOrganizations}</Link>
        </p>
      ) : null}
    </div>
  );
}
