package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.CategoryAlias;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryAliasRepository extends JpaRepository<CategoryAlias, UUID> {

    boolean existsByAliasCode(String aliasCode);
}
