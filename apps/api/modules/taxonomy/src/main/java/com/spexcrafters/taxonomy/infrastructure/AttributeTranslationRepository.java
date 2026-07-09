package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.AttributeTranslation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeTranslationRepository extends JpaRepository<AttributeTranslation, UUID> {

    List<AttributeTranslation> findByAttributeId(UUID attributeId);

    List<AttributeTranslation> findByAttributeIdIn(List<UUID> attributeIds);
}
