package com.spexcrafters.taxonomy.api;

import java.util.List;

/**
 * A category's effective specification template — its own template attributes plus those
 * inherited from ancestor categories (EffectiveSpecificationTemplate schema).
 */
public record EffectiveSpecificationTemplate(
        String categoryCode,
        String templateCode,
        List<SpecificationTemplateAttributeView> attributes) {
}
