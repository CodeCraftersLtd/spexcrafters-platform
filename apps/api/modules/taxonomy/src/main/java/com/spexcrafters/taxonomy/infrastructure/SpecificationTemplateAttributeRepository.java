package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.SpecificationTemplateAttribute;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecificationTemplateAttributeRepository
        extends JpaRepository<SpecificationTemplateAttribute, UUID> {

    List<SpecificationTemplateAttribute> findByTemplateIdOrderBySortOrderAsc(UUID templateId);

    void deleteByTemplateId(UUID templateId);
}
