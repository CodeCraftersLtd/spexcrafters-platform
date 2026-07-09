package com.spexcrafters.supplier.api;

import com.fasterxml.jackson.annotation.JsonValue;
import com.spexcrafters.organizations.api.OrgRole;
import java.util.Set;

/**
 * Closed, typed org-scoped supplier capability set (supplier-domain-model §5). Supplier
 * authorization layers these on the existing organization membership model: a member's org
 * role determines the supplier capabilities they hold. Serialized on the wire with dotted
 * names. These never grant platform moderation (that is the platform-access context).
 *
 * <p>Matrix: OWNER and ADMIN get all capabilities; MEMBER is read-only
 * ({@code supplier.read}, {@code supplier.verification.read}).
 */
public enum SupplierCapability {
    CREATE("supplier.create"),
    READ("supplier.read"),
    UPDATE("supplier.update"),
    SUBMIT("supplier.submit"),
    WITHDRAW("supplier.withdraw"),
    EVIDENCE_READ("supplier.evidence.read"),
    EVIDENCE_UPLOAD("supplier.evidence.upload"),
    EVIDENCE_DELETE("supplier.evidence.delete"),
    VERIFICATION_READ("supplier.verification.read");

    private final String wireName;

    SupplierCapability(String wireName) {
        this.wireName = wireName;
    }

    @JsonValue
    public String wireName() {
        return wireName;
    }

    /** The org-role → supplier-capability matrix. The returned set is immutable. */
    public static Set<SupplierCapability> forRole(OrgRole role) {
        return switch (role) {
            case OWNER, ADMIN -> Set.of(CREATE, READ, UPDATE, SUBMIT, WITHDRAW,
                    EVIDENCE_READ, EVIDENCE_UPLOAD, EVIDENCE_DELETE, VERIFICATION_READ);
            case MEMBER -> Set.of(READ, VERIFICATION_READ);
        };
    }
}
