import type { Dictionary } from '@/lib/i18n';

import type { BffError } from '@/features/auth/client-errors';

export type OrgServerErrors = Dictionary['organizations']['serverErrors'];
export type AcceptInvitationCopy = Dictionary['invitations']['accept'];

/**
 * Resolve an org BFF error code to localized copy: known codes come from the
 * dictionary, unknown codes fall back to the server-provided (already
 * locale-resolved) message, then to the generic error.
 */
export function translateOrgError(
  error: Pick<BffError, 'code' | 'message'>,
  serverErrors: OrgServerErrors,
): string {
  const known = (serverErrors as Record<string, string>)[error.code];
  if (known) {
    return known;
  }
  if (error.message) {
    return error.message;
  }
  return serverErrors.unexpected;
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
 */
export function mapAcceptInvitationError(
  status: number,
  error: Pick<BffError, 'code' | 'message'>,
  copy: AcceptInvitationCopy,
): AcceptInvitationErrorState {
  if (error.code === 'duplicate-membership' || status === 409) {
    return { message: copy.alreadyMember, showOrganizationsLink: true };
  }
  if (error.code === 'invitation-identity-mismatch' || status === 403) {
    return { message: copy.identityMismatch, showOrganizationsLink: false };
  }
  if (error.code === 'token-gone' || status === 410 || status === 404) {
    return { message: copy.expired, showOrganizationsLink: false };
  }
  return { message: copy.genericError, showOrganizationsLink: false };
}
