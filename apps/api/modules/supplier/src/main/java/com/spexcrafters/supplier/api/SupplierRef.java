package com.spexcrafters.supplier.api;

import com.spexcrafters.supplier.domain.OperationalStatus;
import java.util.UUID;

/** A minimal cross-module reference to a supplier (for the verification context). */
public record SupplierRef(UUID id, UUID organizationId, OperationalStatus operationalStatus) {
}
