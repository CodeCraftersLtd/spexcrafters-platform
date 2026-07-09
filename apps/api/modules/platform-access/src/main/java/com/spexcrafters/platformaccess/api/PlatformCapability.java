package com.spexcrafters.platformaccess.api;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Set;

/**
 * Closed, typed platform-staff capability set (supplier-domain-model §6). Platform moderation
 * authority is decided against this enum in the backend application layer — never inferred
 * from an organization role. Serialized on the wire with dotted names. Part of the public
 * {@code api} surface: the supplier and verification contexts pass these to
 * {@link PlatformAccess#require}.
 *
 * <p>Role → capability matrix (this context owns it):
 * <ul>
 *   <li><b>REVIEWER</b> — read the review queue, claim an application, request changes.</li>
 *   <li><b>SENIOR_REVIEWER</b> — everything a REVIEWER can do, plus approve/reject
 *       applications and grant/suspend/revoke verification scopes.</li>
 *   <li><b>PLATFORM_ADMIN</b> — everything a SENIOR_REVIEWER can do, plus suspend an
 *       operational supplier.</li>
 * </ul>
 */
public enum PlatformCapability {
    REVIEW_READ("supplier.review.read"),
    REVIEW_CLAIM("supplier.review.claim"),
    REVIEW_REQUEST_CHANGES("supplier.review.request_changes"),
    REVIEW_APPROVE("supplier.review.approve"),
    REVIEW_REJECT("supplier.review.reject"),
    SUPPLIER_SUSPEND("supplier.suspend"),
    VERIFICATION_GRANT("supplier.verification.grant"),
    VERIFICATION_SUSPEND("supplier.verification.suspend"),
    VERIFICATION_REVOKE("supplier.verification.revoke"),
    TAXONOMY_READ("taxonomy.read"),
    TAXONOMY_WRITE("taxonomy.write"),
    BRAND_APPROVE("taxonomy.brand.approve");

    private final String wireName;

    PlatformCapability(String wireName) {
        this.wireName = wireName;
    }

    @JsonValue
    public String wireName() {
        return wireName;
    }

    /** The role → capability matrix. The returned set is immutable. */
    public static Set<PlatformCapability> forRole(PlatformRole role) {
        return switch (role) {
            case REVIEWER -> Set.of(REVIEW_READ, REVIEW_CLAIM, REVIEW_REQUEST_CHANGES);
            case SENIOR_REVIEWER -> Set.of(REVIEW_READ, REVIEW_CLAIM, REVIEW_REQUEST_CHANGES,
                    REVIEW_APPROVE, REVIEW_REJECT,
                    VERIFICATION_GRANT, VERIFICATION_SUSPEND, VERIFICATION_REVOKE,
                    TAXONOMY_READ);
            case PLATFORM_ADMIN -> Set.of(REVIEW_READ, REVIEW_CLAIM, REVIEW_REQUEST_CHANGES,
                    REVIEW_APPROVE, REVIEW_REJECT, SUPPLIER_SUSPEND,
                    VERIFICATION_GRANT, VERIFICATION_SUSPEND, VERIFICATION_REVOKE,
                    TAXONOMY_READ, TAXONOMY_WRITE, BRAND_APPROVE);
        };
    }
}
