package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.Brand;
import com.spexcrafters.taxonomy.domain.BrandApprovalStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    Optional<Brand> findByCode(String code);

    boolean existsByCode(String code);

    List<Brand> findByApprovalStatusOrderByCanonicalNameAsc(BrandApprovalStatus approvalStatus);

    List<Brand> findAllByOrderByCanonicalNameAsc();
}
