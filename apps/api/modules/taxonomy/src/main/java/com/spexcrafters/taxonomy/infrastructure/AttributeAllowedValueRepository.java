package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.AttributeAllowedValue;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeAllowedValueRepository extends JpaRepository<AttributeAllowedValue, UUID> {

    List<AttributeAllowedValue> findByAttributeIdAndActiveTrueOrderBySortOrderAsc(UUID attributeId);
}
