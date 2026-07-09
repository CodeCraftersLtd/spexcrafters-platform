package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.UnitTranslation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitTranslationRepository extends JpaRepository<UnitTranslation, UUID> {

    List<UnitTranslation> findByUnitCode(String unitCode);

    Optional<UnitTranslation> findByUnitCodeAndLocale(String unitCode, String locale);
}
