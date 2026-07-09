package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.BrandAlias;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandAliasRepository extends JpaRepository<BrandAlias, UUID> {

    List<BrandAlias> findByBrandIdOrderByAliasAsc(UUID brandId);
}
