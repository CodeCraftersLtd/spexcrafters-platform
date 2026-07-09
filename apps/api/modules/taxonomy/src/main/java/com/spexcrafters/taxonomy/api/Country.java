package com.spexcrafters.taxonomy.api;

/** An ISO 3166-1 country with its localized name (Country schema). */
public record Country(
        String code,
        String alpha3,
        String numericCode,
        String region,
        String subregion,
        String continent,
        String name) {
}
