import { describe, expect, it } from 'vitest';

import organizations from '../../../messages/en/organizations.json';
import type { Translator } from '@/i18n/translator';

import { mapAcceptInvitationError, translateOrgError } from './org-errors';

/** Build a next-intl-shaped translator from a flat message record. */
function translator(messages: Record<string, string>): Translator {
  return Object.assign((key: string) => messages[key] ?? key, {
    has: (key: string) => key in messages,
  });
}

const accept = translator(
  organizations.acceptInvitation as unknown as Record<string, string>,
);
const serverErrors = translator(
  organizations.serverErrors as unknown as Record<string, string>,
);
const acceptCopy = organizations.acceptInvitation;
const serverErrorsCopy = organizations.serverErrors;

describe('mapAcceptInvitationError', () => {
  it('maps 410 (expired or consumed token) to the expired message', () => {
    const state = mapAcceptInvitationError(410, { code: 'token-gone', message: '' }, accept);
    expect(state.message).toBe(acceptCopy.expired);
    expect(state.showOrganizationsLink).toBe(false);
  });

  it('maps 403 identity mismatch to the sign-in-with-invited-email message', () => {
    const state = mapAcceptInvitationError(
      403,
      { code: 'invitation-identity-mismatch', message: '' },
      accept,
    );
    expect(state.message).toBe(acceptCopy.identityMismatch);
    expect(state.showOrganizationsLink).toBe(false);
  });

  it('maps 409 already-member to the duplicate message with an organizations link', () => {
    const state = mapAcceptInvitationError(
      409,
      { code: 'duplicate-membership', message: '' },
      accept,
    );
    expect(state.message).toBe(acceptCopy.alreadyMember);
    expect(state.showOrganizationsLink).toBe(true);
  });

  it('prefers the problem code even when the status is unexpected', () => {
    const state = mapAcceptInvitationError(
      400,
      { code: 'duplicate-membership', message: '' },
      accept,
    );
    expect(state.message).toBe(acceptCopy.alreadyMember);
    expect(state.showOrganizationsLink).toBe(true);
  });

  it('falls back to the generic error for unknown failures', () => {
    const state = mapAcceptInvitationError(500, { code: 'unexpected', message: '' }, accept);
    expect(state.message).toBe(acceptCopy.genericError);
    expect(state.showOrganizationsLink).toBe(false);
  });
});

describe('translateOrgError', () => {
  it('resolves known codes from the messages', () => {
    expect(
      translateOrgError({ code: 'last-owner', message: 'raw' }, serverErrors),
    ).toBe(serverErrorsCopy['last-owner']);
  });

  it('falls back to the server message for unknown codes', () => {
    expect(
      translateOrgError({ code: 'brand-new-code', message: 'Localized detail' }, serverErrors),
    ).toBe('Localized detail');
  });

  it('falls back to the generic message when nothing else is available', () => {
    expect(translateOrgError({ code: 'brand-new-code', message: '' }, serverErrors)).toBe(
      serverErrorsCopy.unexpected,
    );
  });
});
