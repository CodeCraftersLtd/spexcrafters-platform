package com.spexcrafters.supplier.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.spexcrafters.organizations.api.OrgMembershipView;
import com.spexcrafters.organizations.api.OrgRole;
import com.spexcrafters.organizations.api.OrganizationDirectory;
import com.spexcrafters.supplier.domain.Supplier;
import com.spexcrafters.supplier.domain.SupplierAuthorizationDeniedException;
import com.spexcrafters.supplier.domain.SupplierNotFoundException;
import com.spexcrafters.supplier.infrastructure.SupplierRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SupplierAccessTest {

    private final SupplierRepository suppliers = mock(SupplierRepository.class);
    private final OrganizationDirectory organizationDirectory = mock(OrganizationDirectory.class);
    private final SupplierAccessDenialAuditor auditor = mock(SupplierAccessDenialAuditor.class);
    private final SupplierAccess access = new SupplierAccess(suppliers, organizationDirectory, auditor);

    private final UUID user = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();
    private final UUID supplierId = UUID.randomUUID();

    private Supplier supplier() {
        return new Supplier(supplierId, orgId, "en");
    }

    @Test
    void unknownSupplierIsConcealedAs404AndAudited() {
        when(suppliers.findById(supplierId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> access.requireForSupplier(user, supplierId, SupplierCapability.READ))
                .isInstanceOf(SupplierNotFoundException.class);
        verify(auditor).recordDenied(eq(user), eq("supplier"), eq(supplierId), eq("supplier.read"), any());
    }

    @Test
    void nonMemberIsConcealedAs404() {
        when(suppliers.findById(supplierId)).thenReturn(Optional.of(supplier()));
        when(organizationDirectory.findActiveMembership(orgId, user)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> access.requireForSupplier(user, supplierId, SupplierCapability.READ))
                .isInstanceOf(SupplierNotFoundException.class);
    }

    @Test
    void memberLackingCapabilityGets403() {
        when(suppliers.findById(supplierId)).thenReturn(Optional.of(supplier()));
        when(organizationDirectory.findActiveMembership(orgId, user))
                .thenReturn(Optional.of(new OrgMembershipView(orgId, user, OrgRole.MEMBER, true)));
        assertThatThrownBy(() -> access.requireForSupplier(user, supplierId, SupplierCapability.UPDATE))
                .isInstanceOf(SupplierAuthorizationDeniedException.class);
        verify(auditor).recordDenied(eq(user), eq("supplier"), eq(supplierId), eq("supplier.update"), eq(orgId));
    }

    @Test
    void memberCanReadWithVerificationReadCapability() {
        when(suppliers.findById(supplierId)).thenReturn(Optional.of(supplier()));
        when(organizationDirectory.findActiveMembership(orgId, user))
                .thenReturn(Optional.of(new OrgMembershipView(orgId, user, OrgRole.MEMBER, true)));
        SupplierContext context = access.requireForSupplier(user, supplierId, SupplierCapability.READ);
        assertThat(context.role()).isEqualTo(OrgRole.MEMBER);
        assertThat(context.has(SupplierCapability.VERIFICATION_READ)).isTrue();
    }

    @Test
    void ownerResolvesFullCapabilitySet() {
        when(suppliers.findById(supplierId)).thenReturn(Optional.of(supplier()));
        when(organizationDirectory.findActiveMembership(orgId, user))
                .thenReturn(Optional.of(new OrgMembershipView(orgId, user, OrgRole.OWNER, true)));
        SupplierContext context = access.requireForSupplier(user, supplierId, SupplierCapability.EVIDENCE_UPLOAD);
        assertThat(context.capabilities()).contains(SupplierCapability.EVIDENCE_UPLOAD,
                SupplierCapability.SUBMIT, SupplierCapability.WITHDRAW);
    }

    @Test
    void createRequiresOwnerOrAdminForOrganization() {
        when(organizationDirectory.findActiveMembership(orgId, user))
                .thenReturn(Optional.of(new OrgMembershipView(orgId, user, OrgRole.MEMBER, true)));
        assertThatThrownBy(() -> access.requireForOrganization(user, orgId, SupplierCapability.CREATE))
                .isInstanceOf(SupplierAuthorizationDeniedException.class);
    }
}
