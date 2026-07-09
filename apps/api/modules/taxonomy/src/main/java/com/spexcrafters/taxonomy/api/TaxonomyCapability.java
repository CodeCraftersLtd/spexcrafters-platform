package com.spexcrafters.taxonomy.api;

/**
 * The taxonomy-administration capabilities surfaced on the wire (matches the OpenAPI
 * {@code TaxonomyCapability} schema). Distinct from the platform-access {@code PlatformCapability}
 * whose dotted wire names are an internal concern; taxonomy responses expose these plain names.
 */
public enum TaxonomyCapability {
    TAXONOMY_READ,
    TAXONOMY_WRITE,
    BRAND_APPROVE
}
