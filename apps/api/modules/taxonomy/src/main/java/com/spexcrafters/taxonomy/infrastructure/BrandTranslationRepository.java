package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.BrandTranslation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandTranslationRepository extends JpaRepository<BrandTranslation, UUID> {

    List<BrandTranslation> findByBrandId(UUID brandId);

    List<BrandTranslation> findByBrandIdIn(List<UUID> brandIds);
}
