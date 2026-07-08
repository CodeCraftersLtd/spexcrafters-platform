import { describe, expect, it } from 'vitest';

import en from '../../../messages/en.json';

import { mapAcceptInvitationError, translateOrgError } from './org-errors';

const copy = en.invitations.accept;
const serverErrors = en.organizations.serverErrors;

describe('mapAcceptInvitationError', () => {
  it('maps 410 (expired or consumed token) to the expired message', () => {
    const state = mapAcceptInvitationError(410, { code: 'token-gone', message: '' }, copy);
    expect(state.message).toBe(copy.expired);
    expect(state.showOrganizationsLink).toBe(false);
  });

  it('maps 403 identity mismatch to the sign-in-with-invited-email message', () => {
    const state = mapAcceptInvitationError(
      403,
      { code: 'invitation-identity-mismatch', message: '' },
      copy,
    );
    expect(state.message).toBe(copy.identityMismatch);
    expect(state.showOrganizationsLink).toBe(false);
  });

  it('maps 409 already-member to the duplicate message with an organizations link', () => {
    const state = mapAcceptInvitationError(
      409,
      { code: 'duplicate-membership', message: '' },
      copy,
    );
    expect(state.message).toBe(copy.alreadyMember);
    expect(state.showOrganizationsLink).toBe(true);
  });

  it('prefers the problem code even when the status is unexpected', () => {
    const state = mapAcceptInvitationError(
      400,
      { code: 'duplicate-membership', message: '' },
      copy,
    );
    expect(state.message).toBe(copy.alreadyMember);
    expect(state.showOrganizationsLink).toBe(true);
  });

  it('falls back to the generic error for unknown failures', () => {
    const state = mapAcceptInvitationError(500, { code: 'unexpected', message: '' }, copy);
    expect(state.message).toBe(copy.genericError);
    expect(state.showOrganizationsLink).toBe(false);
  });
});

describe('translateOrgError', () => {
  it('resolves known codes from the dictionary', () => {
    expect(
      translateOrgError({ code: 'last-owner', message: 'raw' }, serverErrors),
    ).toBe(serverErrors['last-owner']);
  });

  it('falls back to the server message for unknown codes', () => {
    expect(
      translateOrgError({ code: 'brand-new-code', message: 'Localized detail' }, serverErrors),
    ).toBe('Localized detail');
  });

  it('falls back to the generic message when nothing else is available', () => {
    expect(translateOrgError({ code: 'brand-new-code', message: '' }, serverErrors)).toBe(
      serverErrors.unexpected,
    );
  });
});
