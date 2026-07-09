package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnumerationRepository extends JpaRepository<Enumeration, UUID> {

    List<Enumeration> findAllByOrderByCodeAsc();

    Optional<Enumeration> findByCode(String code);

    boolean existsByCode(String code);
}
