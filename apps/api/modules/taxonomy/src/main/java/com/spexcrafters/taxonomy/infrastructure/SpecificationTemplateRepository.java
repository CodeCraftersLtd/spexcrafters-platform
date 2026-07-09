package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.SpecificationTemplate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecificationTemplateRepository extends JpaRepository<SpecificationTemplate, UUID> {

    Optional<SpecificationTemplate> findByCategoryId(UUID categoryId);

    Optional<SpecificationTemplate> findByCode(String code);

    boolean existsByCode(String code);
}
