package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.UnitOfMeasure;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, String> {

    List<UnitOfMeasure> findByActiveTrueOrderBySortOrderAsc();

    List<UnitOfMeasure> findAllByOrderBySortOrderAsc();
}
