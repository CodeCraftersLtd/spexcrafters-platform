package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.CountryTranslation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryTranslationRepository extends JpaRepository<CountryTranslation, UUID> {

    List<CountryTranslation> findByCountryCode(String countryCode);
}
