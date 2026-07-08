package com.spexcrafters.organizations.api;

import com.spexcrafters.organizations.domain.MembershipStatus;
import com.spexcrafters.organizations.domain.Organization;
import com.spexcrafters.organizations.domain.OrganizationMembership;
import com.spexcrafters.organizations.infrastructure.OrganizationMembershipRepository;
import com.spexcrafters.organizations.infrastructure.OrganizationRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The caller's own ACTIVE memberships joined with organization name and type. */
@Service
public class MyOrganizationsService {

    private final OrganizationMembershipRepository memberships;
    private final OrganizationRepository organizations;

    public MyOrganizationsService(OrganizationMembershipRepository memberships,
            OrganizationRepository organizations) {
        this.memberships = memberships;
        this.organizations = organizations;
    }

    @Transactional(readOnly = true)
    public List<MyMembershipDto> list(UUID userId) {
        List<OrganizationMembership> active =
                memberships.findByUserIdAndStatusOrderByJoinedAtAsc(userId, MembershipStatus.ACTIVE);
        Map<UUID, Organization> byId = organizations
                .findAllById(active.stream().map(OrganizationMembership::getOrganizationId).toList())
                .stream()
                .collect(Collectors.toMap(Organization::getId, Function.identity()));
        return active.stream()
                .map(membership -> {
                    Organization organization = byId.get(membership.getOrganizationId());
                    if (organization == null) {
                        throw new IllegalStateException("Membership " + membership.getId()
                                + " references a missing organization");
                    }
                    return new MyMembershipDto(
                            membership.getId(),
                            organization.getId(),
                            organization.getName(),
                            organization.getType(),
                            membership.getRole(),
                            membership.getJoinedAt());
                })
                .toList();
    }
}
