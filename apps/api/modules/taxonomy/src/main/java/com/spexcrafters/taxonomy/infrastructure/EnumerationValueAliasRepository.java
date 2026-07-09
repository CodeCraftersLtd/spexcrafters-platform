package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.EnumerationValueAlias;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnumerationValueAliasRepository extends JpaRepository<EnumerationValueAlias, UUID> {
}
