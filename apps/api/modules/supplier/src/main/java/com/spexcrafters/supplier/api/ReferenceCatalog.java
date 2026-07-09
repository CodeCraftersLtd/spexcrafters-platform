package com.spexcrafters.supplier.api;

import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import com.spexcrafters.sharedkernel.problem.ProblemFieldError;
import com.spexcrafters.supplier.infrastructure.ReferenceCodeRepository;
import com.spexcrafters.supplier.infrastructure.ReferenceCodeRepository.Kind;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Validates class-C taxonomy codes (supplier types, capabilities, evidence types, facility
 * types, verification scopes) against the seeded {@code reference} schema. Domain logic
 * depends on the codes only — never on their translated labels. Exposed on the {@code api}
 * surface so the verification context can validate scope codes too. Unknown or inactive
 * codes are a 422 validation error.
 */
@Service
public class ReferenceCatalog {

    private final ReferenceCodeRepository repository;

    public ReferenceCatalog(ReferenceCodeRepository repository) {
        this.repository = repository;
    }

    public Set<String> supplierTypes() {
        return repository.activeCodes(Kind.SUPPLIER_TYPE);
    }

    public Set<String> verificationScopes() {
        return repository.activeCodes(Kind.VERIFICATION_SCOPE);
    }

    public void requireSupplierType(String code) {
        require(Kind.SUPPLIER_TYPE, "typeCode", code);
    }

    public void requireSupplierCapability(String code) {
        require(Kind.SUPPLIER_CAPABILITY, "capabilityCode", code);
    }

    public void requireEvidenceType(String code) {
        require(Kind.EVIDENCE_TYPE, "evidenceTypeCode", code);
    }

    public void requireFacilityType(String code) {
        require(Kind.FACILITY_TYPE, "facilityTypeCode", code);
    }

    public void requireVerificationScope(String code) {
        require(Kind.VERIFICATION_SCOPE, "scopeCode", code);
    }

    private void require(Kind kind, String field, String code) {
        if (code == null || !repository.activeCodes(kind).contains(code)) {
            throw ApiProblemException.validation(List.of(
                    new ProblemFieldError(field, "UnknownCode", "Unknown or inactive code: " + code)));
        }
    }
}
