package com.spexcrafters.taxonomy.api;

import com.spexcrafters.platformaccess.api.PlatformAccess;
import com.spexcrafters.platformaccess.api.PlatformCapability;
import com.spexcrafters.taxonomy.domain.CountryTranslation;
import com.spexcrafters.taxonomy.infrastructure.CountryRepository;
import com.spexcrafters.taxonomy.infrastructure.CountryTranslationRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ISO 3166-1 country registry reads (public, plus a staff-gated all-status admin read). */
@Service
public class CountryService {

    private final CountryRepository countries;
    private final CountryTranslationRepository translations;
    private final PlatformAccess platformAccess;

    public CountryService(CountryRepository countries, CountryTranslationRepository translations,
            PlatformAccess platformAccess) {
        this.countries = countries;
        this.translations = translations;
        this.platformAccess = platformAccess;
    }

    @Transactional(readOnly = true)
    public List<Country> list(String locale) {
        return countries.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(c -> toDto(c, byCountryCode(), locale))
                .toList();
    }

    /**
     * Administration country list: platform-staff-only (TAXONOMY_READ). Returns EVERY country
     * (including inactive) so staff can review all statuses.
     */
    @Transactional(readOnly = true)
    public List<Country> listForAdmin(UUID userId, String locale) {
        platformAccess.require(userId, PlatformCapability.TAXONOMY_READ);
        return countries.findAllByOrderBySortOrderAsc().stream()
                .map(c -> toDto(c, byCountryCode(), locale))
                .toList();
    }

    private Map<String, List<CountryTranslation>> byCountryCode() {
        return translations.findAll().stream()
                .collect(Collectors.groupingBy(CountryTranslation::getCountryCode));
    }

    private Country toDto(com.spexcrafters.taxonomy.domain.Country c,
            Map<String, List<CountryTranslation>> byCountry, String locale) {
        List<CountryTranslation> rows = byCountry.getOrDefault(c.getCode(), List.of());
        int currentVersion = rows.stream().filter(CountryTranslation::isOriginal)
                .mapToInt(CountryTranslation::getSourceVersion).findFirst().orElse(1);
        var resolved = LocalizationResolver.resolve(rows, locale, currentVersion);
        String name = resolved.isPresent() && resolved.translation().getName() != null
                ? resolved.translation().getName() : c.getCode();
        return new Country(c.getCode(), c.getAlpha3(), c.getNumericCode(), c.getRegion(),
                c.getSubregion(), c.getContinent(), name);
    }
}
