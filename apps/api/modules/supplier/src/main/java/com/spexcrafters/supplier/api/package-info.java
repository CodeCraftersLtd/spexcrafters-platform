/**
 * Public API of the supplier bounded context: application lifecycle, profile and translation
 * management, evidence upload/finalize/download, the reviewer workflow, the org-scoped
 * {@code SupplierAccess} authorization policy, reference/locale catalogs, and the cross-module
 * directories consumed by the verification context. Cross-module access is permitted only
 * through this {@code ...api} package (ArchUnit-enforced).
 */
package com.spexcrafters.supplier.api;
