package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.Certification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificationRepository extends JpaRepository<Certification, UUID> {

    List<Certification> findAllByOrderBySortOrderAsc();

    Optional<Certification> findByCode(String code);

    boolean existsByCode(String code);
}
