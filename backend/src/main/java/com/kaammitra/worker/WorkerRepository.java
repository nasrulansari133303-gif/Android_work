package com.kaammitra.worker;

import com.kaammitra.common.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkerRepository extends JpaRepository<WorkerEntity, Long> {
    Optional<WorkerEntity> findByUserId(Long userId);
    long countByVerificationStatus(VerificationStatus status);
    List<WorkerEntity> findByVerificationStatusOrderByIdDesc(VerificationStatus status);

    @Query("""
        SELECT DISTINCT w FROM WorkerEntity w
        JOIN WorkerServiceEntity ws ON ws.worker.id = w.id
        WHERE w.verificationStatus = com.kaammitra.common.VerificationStatus.VERIFIED
          AND w.online = true
          AND w.busy = false
          AND w.user.active = true
          AND ws.service.id = :serviceId
          AND ws.service.active = true
          AND w.currentLatitude IS NOT NULL
          AND w.currentLongitude IS NOT NULL
    """)
    List<WorkerEntity> findAvailableOnlineWorkersForService(@Param("serviceId") Long serviceId);
}
