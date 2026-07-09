package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.Country;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, String> {

    List<Country> findByActiveTrueOrderBySortOrderAsc();
}
