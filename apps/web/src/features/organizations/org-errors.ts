import type { Translator } from '@/i18n/translator';

import type { BffError } from '@/features/auth/client-errors';

/**
 * Resolve an org BFF error code to localized copy from the
 * `organizations.serverErrors` namespace: known codes come from messages,
 * unknown codes fall back to the server-provided (already locale-resolved)
 * message, then to the generic error. `t` is scoped to
 * `organizations.serverErrors`.
 */
export function translateOrgError(
  error: Pick<BffError, 'code' | 'message'>,
  t: Translator,
): string {
  if (t.has(error.code)) {
    return t(error.code);
  }
  if (error.message) {
    return error.message;
  }
  return t('unexpected');
}

export interface AcceptInvitationErrorState {
  message: string;
  /** 409 duplicate-membership: the user already belongs — link to the list. */
  showOrganizationsLink: boolean;
}

/**
 * Map an /api/invitations/accept failure to its designed error state:
 * 410 (expired/consumed/revoked token), 403 identity mismatch (sign in with
 * the invited email), 409 already-member (link to the organization list).
 * `t` is a next-intl translator scoped to `organizations.acceptInvitation`.
 */
export function mapAcceptInvitationError(
  status: number,
  error: Pick<BffError, 'code' | 'message'>,
  t: Translator,
): AcceptInvitationErrorState {
  if (error.code === 'duplicate-membership' || status === 409) {
    return { message: t('alreadyMember'), showOrganizationsLink: true };
  }
  if (error.code === 'invitation-identity-mismatch' || status === 403) {
    return { message: t('identityMismatch'), showOrganizationsLink: false };
  }
  if (error.code === 'token-gone' || status === 410 || status === 404) {
    return { message: t('expired'), showOrganizationsLink: false };
  }
  return { message: t('genericError'), showOrganizationsLink: false };
}
