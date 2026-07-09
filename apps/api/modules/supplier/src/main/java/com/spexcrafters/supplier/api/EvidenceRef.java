package com.spexcrafters.supplier.api;

import java.util.UUID;

/**
 * A minimal cross-module reference to a piece of evidence, for the verification context to
 * validate evidence linkage on a scope grant without reaching into the supplier domain.
 */
public record EvidenceRef(UUID id, UUID supplierId, String evidenceTypeCode, boolean finalized) {
}
