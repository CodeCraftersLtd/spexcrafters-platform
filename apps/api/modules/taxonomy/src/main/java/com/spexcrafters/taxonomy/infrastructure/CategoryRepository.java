package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findByCode(String code);

    boolean existsByCode(String code);

    List<Category> findAllByOrderBySortOrderAsc();

    List<Category> findAllByOrderByPathAsc();

    List<Category> findByActiveTrueOrderBySortOrderAsc();

    List<Category> findByParentId(UUID parentId);
}
