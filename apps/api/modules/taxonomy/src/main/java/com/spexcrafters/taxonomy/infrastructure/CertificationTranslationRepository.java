package com.spexcrafters.taxonomy.infrastructure;

import com.spexcrafters.taxonomy.domain.CertificationTranslation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificationTranslationRepository extends JpaRepository<CertificationTranslation, UUID> {

    List<CertificationTranslation> findByCertificationId(UUID certificationId);
}
