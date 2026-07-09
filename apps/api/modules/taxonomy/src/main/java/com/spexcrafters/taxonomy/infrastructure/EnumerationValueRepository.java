package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.EnumerationValue;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnumerationValueRepository extends JpaRepository<EnumerationValue, UUID> {

    List<EnumerationValue> findByEnumerationIdOrderBySortOrderAsc(UUID enumerationId);

    Optional<EnumerationValue> findByEnumerationIdAndCode(UUID enumerationId, String code);

    boolean existsByEnumerationIdAndCode(UUID enumerationId, String code);

    long countByEnumerationId(UUID enumerationId);
}
