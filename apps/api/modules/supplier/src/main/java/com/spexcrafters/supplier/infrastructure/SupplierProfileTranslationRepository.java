package com.spexcrafters.supplier.infrastructure;

import com.spexcrafters.supplier.domain.SupplierProfileTranslation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierProfileTranslationRepository
        extends JpaRepository<SupplierProfileTranslation, UUID> {

    List<SupplierProfileTranslation> findByProfileIdOrderByLocaleAsc(UUID profileId);

    Optional<SupplierProfileTranslation> findByProfileIdAndLocale(UUID profileId, String locale);
}
