import type { Capability, OrganizationRole } from '@spexcrafters/api-client';

/**
 * UI-side mirror of the capability/rank rules from
 * docs/architecture/organizations-capability-model.md. These decide only what
 * controls to *render* — the backend application service is the authority and
 * re-checks every mutation.
 */

/** Strict rank order OWNER > ADMIN > MEMBER. */
export const ROLE_RANK: Record<OrganizationRole, number> = {
  OWNER: 3,
  ADMIN: 2,
  MEMBER: 1,
};

export function hasCapability(
  capabilities: readonly Capability[],
  capability: Capability,
): boolean {
  return capabilities.includes(capability);
}

/**
 * Whether to show a remove control for a member row.
 * - Self: always show "leave" (self-removal is permitted except for the last
 *   owner, which the backend enforces with a 409 `last-owner`).
 * - Others: requires organization.members.remove; OWNER may target anyone,
 *   ADMIN only strictly-lower ranks (i.e. MEMBER).
 */
export function canRemoveMember(
  callerRole: OrganizationRole,
  capabilities: readonly Capability[],
  targetRole: OrganizationRole,
  isSelf: boolean,
): boolean {
  if (isSelf) {
    return true;
  }
  if (!hasCapability(capabilities, 'organization.members.remove')) {
    return false;
  }
  if (callerRole === 'OWNER') {
    return true;
  }
  return ROLE_RANK[targetRole] < ROLE_RANK[callerRole];
}

/** Role changes require organization.roles.manage and never target oneself. */
export function canChangeRole(
  capabilities: readonly Capability[],
  isSelf: boolean,
): boolean {
  return !isSelf && hasCapability(capabilities, 'organization.roles.manage');
}

export type InvitableRole = 'ADMIN' | 'MEMBER';

/**
 * Role options for the invite form: MEMBER always (when the caller can invite
 * at all); ADMIN only for callers holding organization.roles.manage (per the
 * matrix, ADMIN actors may create MEMBER-role invitations only).
 */
export function invitableRoles(
  capabilities: readonly Capability[],
): InvitableRole[] {
  if (!hasCapability(capabilities, 'organization.members.invite')) {
    return [];
  }
  return hasCapability(capabilities, 'organization.roles.manage')
    ? ['MEMBER', 'ADMIN']
    : ['MEMBER'];
}
