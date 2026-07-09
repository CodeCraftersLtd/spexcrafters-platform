/**
 * E2E message shim. The app migrated to per-namespace message files
 * (messages/en/{namespace}.json, Phase 7); these specs assert against the same
 * English source. This assembles the legacy flat shape the specs read
 * (`en.auth.*`, `en.organizations.*`, `en.invitations.accept.*`, `en.home.*`,
 * `en.buyer.logout`) from the namespace files so the smoke expectations stay in
 * lock-step with the shipped copy.
 */
import auth from '../messages/en/auth.json';
import common from '../messages/en/common.json';
import evidence from '../messages/en/evidence.json';
import navigation from '../messages/en/navigation.json';
import organizations from '../messages/en/organizations.json';
import reviewer from '../messages/en/reviewer.json';
import suppliers from '../messages/en/suppliers.json';
import taxonomy from '../messages/en/taxonomy.json';
import taxonomyAdmin from '../messages/en/taxonomyAdmin.json';
import verification from '../messages/en/verification.json';

export const en = {
  auth,
  home: common.home,
  buyer: { logout: navigation.logout },
  organizations: { ...organizations, logout: navigation.logout },
  invitations: { accept: organizations.acceptInvitation },
  suppliers,
  reviewer,
  evidence,
  verification,
  taxonomy,
  taxonomyAdmin,
};
