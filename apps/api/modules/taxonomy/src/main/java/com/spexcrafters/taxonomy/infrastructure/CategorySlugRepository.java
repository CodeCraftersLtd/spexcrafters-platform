package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.CategorySlug;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategorySlugRepository extends JpaRepository<CategorySlug, UUID> {

    Optional<CategorySlug> findByLocaleAndSlug(String locale, String slug);

    Optional<CategorySlug> findByCategoryIdAndLocaleAndPrimaryTrueAndActiveTrue(UUID categoryId, String locale);

    List<CategorySlug> findByCategoryIdAndLocale(UUID categoryId, String locale);

    List<CategorySlug> findByPrimaryTrueAndActiveTrue();
}
