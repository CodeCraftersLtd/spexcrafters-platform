package com.spexcrafters.taxonomy.api;

import com.spexcrafters.taxonomy.domain.CountryTranslation;
import com.spexcrafters.taxonomy.infrastructure.CountryRepository;
import com.spexcrafters.taxonomy.infrastructure.CountryTranslationRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ISO 3166-1 country registry reads (public). No administration in Phase 8 (seeded set). */
@Service
public class CountryService {

    private final CountryRepository countries;
    private final CountryTranslationRepository translations;

    public CountryService(CountryRepository countries, CountryTranslationRepository translations) {
        this.countries = countries;
        this.translations = translations;
    }

    @Transactional(readOnly = true)
    public List<Country> list(String locale) {
        Map<String, List<CountryTranslation>> byCountry = translations.findAll().stream()
                .collect(Collectors.groupingBy(CountryTranslation::getCountryCode));
        return countries.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(c -> {
                    List<CountryTranslation> rows = byCountry.getOrDefault(c.getCode(), List.of());
                    int currentVersion = rows.stream().filter(CountryTranslation::isOriginal)
                            .mapToInt(CountryTranslation::getSourceVersion).findFirst().orElse(1);
                    var resolved = LocalizationResolver.resolve(rows, locale, currentVersion);
                    String name = resolved.isPresent() && resolved.translation().getName() != null
                            ? resolved.translation().getName() : c.getCode();
                    return new Country(c.getCode(), c.getAlpha3(), c.getNumericCode(), c.getRegion(),
                            c.getSubregion(), c.getContinent(), name);
                })
                .toList();
    }
}
