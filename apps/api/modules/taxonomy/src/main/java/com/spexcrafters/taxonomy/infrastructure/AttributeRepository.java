package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.Attribute;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeRepository extends JpaRepository<Attribute, UUID> {

    Optional<Attribute> findByCode(String code);

    boolean existsByCode(String code);

    List<Attribute> findAllByOrderBySortOrderAsc();

    List<Attribute> findByIdIn(List<UUID> ids);
}
