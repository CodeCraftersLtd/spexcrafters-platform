/**
 * Public API of the taxonomy bounded context (Optical Taxonomy &amp; Specification Registry,
 * Phase 8): read services with the ADR-020 localization fallback chain, write services for
 * every registry aggregate (categories, enumerations, attributes, units, countries,
 * certifications, brands, specification templates), the pure {@code SpecificationValidator}
 * engine, and the response DTOs the {@code ...web} controllers return. Cross-module access is
 * permitted only through this {@code ...api} package (ArchUnit-enforced).
 */
package com.spexcrafters.taxonomy.api;
