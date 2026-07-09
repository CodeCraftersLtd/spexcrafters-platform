package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.CategoryTranslation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryTranslationRepository extends JpaRepository<CategoryTranslation, UUID> {

    List<CategoryTranslation> findByCategoryId(UUID categoryId);

    List<CategoryTranslation> findByCategoryIdIn(List<UUID> categoryIds);
}
