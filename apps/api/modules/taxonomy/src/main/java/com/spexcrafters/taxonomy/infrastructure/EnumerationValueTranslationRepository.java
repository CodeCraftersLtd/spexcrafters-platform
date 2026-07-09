package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.EnumerationValueTranslation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnumerationValueTranslationRepository
        extends JpaRepository<EnumerationValueTranslation, UUID> {

    List<EnumerationValueTranslation> findByEnumerationValueId(UUID enumerationValueId);

    List<EnumerationValueTranslation> findByEnumerationValueIdIn(List<UUID> enumerationValueIds);
}
