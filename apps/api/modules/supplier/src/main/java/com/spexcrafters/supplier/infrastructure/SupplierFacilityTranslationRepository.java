package com.spexcrafters.supplier.infrastructure;

import com.spexcrafters.supplier.domain.SupplierFacilityTranslation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierFacilityTranslationRepository
        extends JpaRepository<SupplierFacilityTranslation, UUID> {

    List<SupplierFacilityTranslation> findByFacilityIdOrderByLocaleAsc(UUID facilityId);

    Optional<SupplierFacilityTranslation> findByFacilityIdAndLocale(UUID facilityId, String locale);
}
