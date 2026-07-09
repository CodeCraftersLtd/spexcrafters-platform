package com.spexcrafters.platformaccess.infrastructure;

import com.spexcrafters.platformaccess.domain.PlatformStaff;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformStaffRepository extends JpaRepository<PlatformStaff, UUID> {

    Optional<PlatformStaff> findByUserId(UUID userId);
}
