package com.spexcrafters.taxonomy.api;

import com.spexcrafters.platformaccess.api.PlatformCapability;
import com.spexcrafters.platformaccess.api.PlatformStaffContext;
import java.util.List;

/** Maps the caller's platform-access grant to the taxonomy capabilities exposed on the wire. */
final class TaxonomyCapabilities {

    private TaxonomyCapabilities() {
    }

    static List<TaxonomyCapability> of(PlatformStaffContext context) {
        List<TaxonomyCapability> caps = new java.util.ArrayList<>();
        if (context.has(PlatformCapability.TAXONOMY_READ)) {
            caps.add(TaxonomyCapability.TAXONOMY_READ);
        }
        if (context.has(PlatformCapability.TAXONOMY_WRITE)) {
            caps.add(TaxonomyCapability.TAXONOMY_WRITE);
        }
        if (context.has(PlatformCapability.BRAND_APPROVE)) {
            caps.add(TaxonomyCapability.BRAND_APPROVE);
        }
        return caps;
    }
}
