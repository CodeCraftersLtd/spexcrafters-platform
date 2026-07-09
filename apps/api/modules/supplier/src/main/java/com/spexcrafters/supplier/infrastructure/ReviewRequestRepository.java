package com.spexcrafters.supplier.infrastructure;

import com.spexcrafters.supplier.domain.ReviewRequest;
import com.spexcrafters.supplier.domain.ReviewRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRequestRepository extends JpaRepository<ReviewRequest, UUID> {

    List<ReviewRequest> findByApplicationIdOrderByRequestedAtAsc(UUID applicationId);

    Optional<ReviewRequest> findByIdAndApplicationId(UUID id, UUID applicationId);

    long countByApplicationIdAndStatus(UUID applicationId, ReviewRequestStatus status);
}
