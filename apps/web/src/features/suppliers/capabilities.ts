import type { SupplierApplicationStatus, SupplierCapability } from '@spexcrafters/api-client';

/**
 * UI-side mirror of the org-scoped supplier capability model. Decides only what
 * controls to *render*; the backend application service re-checks every
 * mutation. `callerCapabilities` comes from SupplierApplication.
 */
export function hasSupplierCapability(
  capabilities: readonly SupplierCapability[],
  capability: SupplierCapability,
): boolean {
  return capabilities.includes(capability);
}

/** Draft edits are permitted only while DRAFT or CHANGES_REQUESTED. */
export function isDraftEditable(status: SupplierApplicationStatus): boolean {
  return status === 'DRAFT' || status === 'CHANGES_REQUESTED';
}

/** Submit/resubmit is offered from DRAFT or CHANGES_REQUESTED with the capability. */
export function canSubmit(
  status: SupplierApplicationStatus,
  capabilities: readonly SupplierCapability[],
): boolean {
  return (
    isDraftEditable(status) && hasSupplierCapability(capabilities, 'supplier.submit')
  );
}

/** Withdraw is offered from any pre-decision state with the capability. */
export function canWithdraw(
  status: SupplierApplicationStatus,
  capabilities: readonly SupplierCapability[],
): boolean {
  const terminal: SupplierApplicationStatus[] = ['APPROVED', 'REJECTED', 'WITHDRAWN'];
  return (
    !terminal.includes(status) &&
    hasSupplierCapability(capabilities, 'supplier.withdraw')
  );
}
