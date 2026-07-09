package com.spexcrafters.verification.api;

import com.spexcrafters.audit.api.AuditLogger;
import com.spexcrafters.platformaccess.api.PlatformAccess;
import com.spexcrafters.platformaccess.api.PlatformCapability;
import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import com.spexcrafters.sharedkernel.problem.ProblemFieldError;
import com.spexcrafters.sharedkernel.util.UuidV7;
import com.spexcrafters.supplier.api.EvidenceRef;
import com.spexcrafters.supplier.api.EvidenceService;
import com.spexcrafters.supplier.api.ReferenceCatalog;
import com.spexcrafters.supplier.api.SupplierAccess;
import com.spexcrafters.supplier.api.SupplierCapability;
import com.spexcrafters.supplier.api.SupplierDirectory;
import com.spexcrafters.verification.domain.ScopeResultEvidence;
import com.spexcrafters.verification.domain.VerificationCase;
import com.spexcrafters.verification.domain.VerificationNotFoundException;
import com.spexcrafters.verification.domain.VerificationScopeResult;
import com.spexcrafters.verification.domain.VerificationStatus;
import com.spexcrafters.verification.infrastructure.ScopeResultEvidenceRepository;
import com.spexcrafters.verification.infrastructure.VerificationCaseRepository;
import com.spexcrafters.verification.infrastructure.VerificationScopeResultRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verification cases and scope results (supplier-domain-model §3, §8). Scope grants require a
 * platform capability plus evidence linkage — a declared capability is never auto-verified,
 * and there is no {@code verified} boolean. Reads are org-scoped via the supplier
 * authorization policy; grant/suspend/revoke are platform-staff-only. History is append-only.
 */
@Service
public class VerificationService {

    private final VerificationCaseRepository cases;
    private final VerificationScopeResultRepository scopeResults;
    private final ScopeResultEvidenceRepository scopeResultEvidence;
    private final SupplierAccess supplierAccess;
    private final SupplierDirectory supplierDirectory;
    private final ReferenceCatalog referenceCatalog;
    private final EvidenceService evidenceService;
    private final PlatformAccess platformAccess;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public VerificationService(VerificationCaseRepository cases,
            VerificationScopeResultRepository scopeResults,
            ScopeResultEvidenceRepository scopeResultEvidence,
            SupplierAccess supplierAccess,
            SupplierDirectory supplierDirectory,
            ReferenceCatalog referenceCatalog,
            EvidenceService evidenceService,
            PlatformAccess platformAccess,
            AuditLogger auditLogger,
            Clock clock) {
        this.cases = cases;
        this.scopeResults = scopeResults;
        this.scopeResultEvidence = scopeResultEvidence;
        this.supplierAccess = supplierAccess;
        this.supplierDirectory = supplierDirectory;
        this.referenceCatalog = referenceCatalog;
        this.evidenceService = evidenceService;
        this.platformAccess = platformAccess;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public VerificationStatusDto getStatus(UUID userId, UUID supplierId) {
        supplierAccess.requireForSupplier(userId, supplierId, SupplierCapability.VERIFICATION_READ);
        return buildStatus(supplierId);
    }

    @Transactional
    public VerificationStatusDto grantScope(UUID userId, UUID supplierId, String scopeCode,
            List<UUID> evidenceIds, Instant validUntil, String reason) {
        platformAccess.require(userId, PlatformCapability.VERIFICATION_GRANT);
        requireSupplier(supplierId);
        referenceCatalog.requireVerificationScope(scopeCode);
        List<UUID> linked = validateEvidence(supplierId, evidenceIds);

        VerificationCase verificationCase = cases.findBySupplierId(supplierId).orElseGet(() -> {
            VerificationCase created = new VerificationCase(UuidV7.generate(), supplierId, clock.instant());
            created.setCreatedBy(userId);
            created.setUpdatedBy(userId);
            return cases.save(created);
        });
        verificationCase.markStatus(VerificationStatus.VERIFIED);
        verificationCase.setUpdatedBy(userId);

        VerificationScopeResult result = scopeResults
                .findByCaseIdAndScopeCode(verificationCase.getId(), scopeCode)
                .orElseGet(() -> {
                    VerificationScopeResult created = new VerificationScopeResult(
                            UuidV7.generate(), verificationCase.getId(), scopeCode);
                    created.setCreatedBy(userId);
                    return created;
                });
        result.grant(userId, clock.instant(), validUntil, reason);
        result.setUpdatedBy(userId);
        scopeResults.save(result);

        scopeResultEvidence.deleteByScopeResultId(result.getId());
        for (UUID evidenceId : linked) {
            ScopeResultEvidence link = new ScopeResultEvidence(UuidV7.generate(), result.getId(), evidenceId);
            link.setCreatedBy(userId);
            link.setUpdatedBy(userId);
            scopeResultEvidence.save(link);
            evidenceService.retain(evidenceId);
        }

        auditLogger.record("supplier.verification.granted", userId, "verification_scope_result",
                result.getId().toString(), Map.of("supplierId", supplierId.toString(),
                        "scopeCode", scopeCode, "evidenceCount", Integer.toString(linked.size())));
        return buildStatus(supplierId);
    }

    @Transactional
    public VerificationStatusDto suspendScope(UUID userId, UUID supplierId, String scopeCode, String reason) {
        VerificationScopeResult result = mutateScope(userId, supplierId, scopeCode,
                PlatformCapability.VERIFICATION_SUSPEND);
        result.suspend(userId, clock.instant(), reason);
        result.setUpdatedBy(userId);
        auditLogger.record("supplier.verification.suspended", userId, "verification_scope_result",
                result.getId().toString(), Map.of("supplierId", supplierId.toString(), "scopeCode", scopeCode));
        return buildStatus(supplierId);
    }

    @Transactional
    public VerificationStatusDto revokeScope(UUID userId, UUID supplierId, String scopeCode, String reason) {
        VerificationScopeResult result = mutateScope(userId, supplierId, scopeCode,
                PlatformCapability.VERIFICATION_REVOKE);
        result.revoke(userId, clock.instant(), reason);
        result.setUpdatedBy(userId);
        auditLogger.record("supplier.verification.revoked", userId, "verification_scope_result",
                result.getId().toString(), Map.of("supplierId", supplierId.toString(), "scopeCode", scopeCode));
        return buildStatus(supplierId);
    }

    // ---------------------------------------------------------------- internals

    private VerificationScopeResult mutateScope(UUID userId, UUID supplierId, String scopeCode,
            PlatformCapability capability) {
        platformAccess.require(userId, capability);
        requireSupplier(supplierId);
        VerificationCase verificationCase = cases.findBySupplierId(supplierId)
                .orElseThrow(VerificationNotFoundException::new);
        return scopeResults.findByCaseIdAndScopeCode(verificationCase.getId(), scopeCode)
                .orElseThrow(VerificationNotFoundException::new);
    }

    private void requireSupplier(UUID supplierId) {
        if (supplierDirectory.findSupplier(supplierId).isEmpty()) {
            throw new VerificationNotFoundException();
        }
    }

    private List<UUID> validateEvidence(UUID supplierId, List<UUID> evidenceIds) {
        if (evidenceIds == null || evidenceIds.isEmpty()) {
            throw ApiProblemException.validation(List.of(new ProblemFieldError(
                    "evidenceIds", "Required", "A verification grant requires at least one evidence item.")));
        }
        List<UUID> linked = new ArrayList<>();
        for (UUID evidenceId : evidenceIds) {
            Optional<EvidenceRef> ref = evidenceService.findForSupplier(evidenceId, supplierId);
            if (ref.isEmpty() || !ref.get().finalized()) {
                throw ApiProblemException.validation(List.of(new ProblemFieldError(
                        "evidenceIds", "InvalidEvidence",
                        "Evidence " + evidenceId + " is not finalized evidence of this supplier.")));
            }
            if (!linked.contains(evidenceId)) {
                linked.add(evidenceId);
            }
        }
        return linked;
    }

    private VerificationStatusDto buildStatus(UUID supplierId) {
        Optional<VerificationCase> verificationCase = cases.findBySupplierId(supplierId);
        if (verificationCase.isEmpty()) {
            return new VerificationStatusDto(supplierId, VerificationStatus.NOT_REQUESTED, null, List.of());
        }
        VerificationCase c = verificationCase.get();
        List<VerificationScopeResultDto> scopes = scopeResults.findByCaseIdOrderByScopeCodeAsc(c.getId())
                .stream()
                .map(result -> new VerificationScopeResultDto(
                        result.getScopeCode(), result.getStatus(), result.getDecidedAt(), result.getValidFrom(),
                        result.getValidUntil(), result.getReason(),
                        scopeResultEvidence.findByScopeResultId(result.getId()).stream()
                                .map(ScopeResultEvidence::getEvidenceId).toList()))
                .toList();
        return new VerificationStatusDto(supplierId, c.getStatus(), c.getOpenedAt(), scopes);
    }
}
